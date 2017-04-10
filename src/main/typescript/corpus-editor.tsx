//
// src/main/typescript/corpus-editor.tsx
// RegEx Frontend
//
// Created on 4/7/17
//
// BUG: "add highlight" popovers linger after mouse has moved away
//

import 'codemirror'
import { Component, ReactNode } from 'react'
import { Overlay } from './overlay'
import { StartGrip, EndGrip } from './grip'
import { Popover } from './popover'
import { Button } from './button'
import { PointPair, Point } from './point'
import { Highlight } from './highlight'
import { HighlightList } from './highlight-list'
import * as util from './util'

const CM_COORD_SYSTEM = 'local'
const CM_HIGHLIGHT_CLASS = 'marked-highlight'

interface Props {
  regex: string
  corpus: string
  onMatchesChange: (matches: { start: number, end: number }[]) => void
  onInfiniteRegex: () => void
  onBrokenRegex: () => void
}

interface State {
  regex: string
}

export class CorpusEditor extends Component<Props, State> {
  private textarea: HTMLTextAreaElement
  private instance: CodeMirror.Editor
  private document: CodeMirror.Doc
  private highlights: HighlightList
  private isDragging: boolean = false
  private popovers: ReactNode[] = []
  private popoverTimeout: number

  constructor (props) {
    super(props)

    this.state = {
      regex: props.regex,
    }
  }

  componentDidMount () {
    this.instance = window['CodeMirror'].fromTextArea(this.textarea)
    this.instance.setValue(this.props.corpus)
    this.document = this.instance.getDoc()
    this.instance.on('change', this.handleEditorChange.bind(this))
    this.instance.on('cursorActivity', this.handleCursorActivity.bind(this))
    this.resetHighlights()
  }

  componentWillReceiveProps (nextProps: Props): void {
    this.setState({
      regex: nextProps.regex,
    })
  }

  componentDidUpdate (prevProps: Props, prevState: State) {
    if (prevState.regex  !== this.state.regex) {
      this.resetHighlights()
    }
  }

  private isValidRegex (regex: string): boolean {
    try {
      new RegExp(regex, 'g')
      return true
    } catch (err) {
      return false
    }
  }

  private handleEditorChange () {
    this.resetHighlights()
  }

  private handleCursorActivity () {
    if (this.document.somethingSelected()) {
      let selections = this.document.listSelections()
      let lastSelection = selections[selections.length - 1]

      let startPos = lastSelection.anchor
      let endPos = lastSelection.head

      if (util.lessThanPosition(endPos, startPos)) {
        startPos = lastSelection.head
        endPos = lastSelection.anchor
      }

      let startIndex  = this.document.indexFromPos(startPos)
      let startCoords = this.instance.charCoords(startPos, CM_COORD_SYSTEM)
      let start       = { index: startIndex, pos: startPos, coords: startCoords }

      let endIndex    = this.document.indexFromPos(endPos)
      let endCoords   = this.instance.charCoords(endPos, CM_COORD_SYSTEM)
      let end         = { index: endIndex, pos: endPos, coords: endCoords }

      this.hideAllPopovers()
      this.showAddPopover({ start: start, end: end })
    } else {
      this.hideAllPopovers()
    }
  }

  // Draws CodeMirror text markers from an existing list of highlights.
  private drawMarkers (): void {
    if (this.highlights) {
      this.highlights.forEach((h) => {
        let mark = this.markTextWithPointPair(h.getPair())
        h.setMark(mark)
      })
    }
  }

  private attachMarkerListeners (): void {
    /**
     * The `setTimeout` call is used because *I THINK* that it takes some time
     * to update the CodeMirror DOM with the appropriate text marking <span>s
     * and the callback with a delay of 0 milliseconds places this callback in
     * the event queue so it doesn't immediately but waits until all pending
     * operations have completed. It's also possible I'm being an idiot and
     * this is just a horrible hack that's obscuring a deeper bug.
     */
    setTimeout(() => {
      let elems = document.querySelectorAll('.' + CM_HIGHLIGHT_CLASS)

      this.highlights.forEach((h, i) => {
        let elem = elems[i]
        elem.addEventListener('mouseover', this.showRemovePopover.bind(this, h))
        elem.addEventListener('mouseout', this.delayHideAllPopovers.bind(this))
      })
    }, 0)
  }

  private showPopover (pair: PointPair, child: ReactNode): void {
    if (this.isDragging === false) {
      let cancelHideAllPopovers = this.cancelHideAllPopovers.bind(this)
      let delayHideAllPopovers  = this.delayHideAllPopovers.bind(this)

      this.hideAllPopovers()
      this.popovers.push(
        <Popover
          key={this.popovers.length}
          pair={pair}
          onMouseOver={cancelHideAllPopovers}
          onMouseOut={delayHideAllPopovers}>
          {child}
        </Popover>
      )
      this.forceUpdate()
    }
  }

  private showRemovePopover (h: Highlight) {
    this.showPopover(h.getPair(), (
      <Button
        glyph="\u2717"
        color="red"
        arrow={true}
        onClick={() => {
          this.hideAllPopovers()
          this.removeHighlight(h)
        }} />
    ))
  }

  private showAddPopover (pair: PointPair) {
    this.showPopover(pair, (
      <Button
        glyph="\u2713"
        color="green"
        arrow={true}
        onClick={() => {
          this.hideAllPopovers()
          this.addHighlight(pair)
        }} />
    ))
  }

  private delayHideAllPopovers () {
    this.popoverTimeout = setTimeout(this.hideAllPopovers.bind(this), 500)
  }

  private cancelHideAllPopovers () {
    clearTimeout(this.popoverTimeout)
  }

  private hideAllPopovers () {
    this.popovers = []
    this.cancelHideAllPopovers()
    this.forceUpdate()
  }

  private removeHighlight (h: Highlight) {
    if (this.highlights) {
      this.clearMarkers()
      this.highlights.remove(h)
      this.drawMarkers()
      this.attachMarkerListeners()
      this.props.onMatchesChange(this.highlights.getMatches())
      this.forceUpdate()
    }
  }

  private addHighlight (pair: PointPair): void {
    if (!this.highlights) {
      this.highlights = new HighlightList()
    }

    let mark = this.markTextWithPointPair(pair)
    let highlight = new Highlight(pair, mark)

    try {
      this.highlights.insert(highlight)
    } catch (err) {
      mark.clear()
      alert('matches cannot overlap')
      return
    }

    this.attachMarkerListeners()
    this.props.onMatchesChange(this.highlights.getMatches())
    this.forceUpdate()
  }

  // Removes just the text markers within CodeMirror.
  private clearMarkers (): void {
    if (this.highlights) {
      this.highlights.forEach((h) => {
        h.getMark().clear()
      })
    }
  }

  // Removes text markers and data structure tracking highlights in corpus.
  private clearHighlights (): void {
    this.clearMarkers()
    this.highlights = null
  }

  // Removes any existing highlights & text markers, re-computes matches from
  // existing regular expression & applies those matches to the document. Will
  // destory any modifications made to previous highlights.
  private resetHighlights (): void {
    // Remove any existing text marks to ensure a fresh render.
    this.clearHighlights()

    // Find indices in the corpus that match the regex.
    let pairs = this.getMatchingPointPairs()

    if (pairs !== undefined) {
      // Create new Highlight and add it to the global list of Highlights.
      this.highlights = pairs.reduce((list, pair, i) => {
        let mark = this.markTextWithPointPair(pair)
        let highlight = new Highlight(pair, mark)
        return list.insert(highlight)
      }, new HighlightList())

      // Listen for mouse-over events on the text marker elements.
      this.attachMarkerListeners()
      this.props.onMatchesChange(this.highlights.getMatches())
    }

    this.forceUpdate()
  }

  private handleInfiniteMatches (): void {
    this.props.onInfiniteRegex()
  }

  private handleBrokenRegex (): void {
    this.props.onBrokenRegex()
  }

  // Runs a regular expression over the corpus and generates a list of pairs of
  // coordinates where each pair corresponds to a region of the corpus that
  // matched the regular expression. Causes no side effects.
  private getMatchingPointPairs (): PointPair[] | undefined {
    let regex: RegExp

    try {
      regex = new RegExp(this.state.regex, 'g')
    } catch (err) {
      return void this.handleBrokenRegex()
    }

    // Collect a list of start/end indices corresponding to matches.
    let indices: { start: number, end: number}[] = []
    let index = 0
    let match = null
    while (true) {
      if ((match = regex.exec(this.instance.getValue())) == null) {
        // No more matches in the corpus.
        break
      }

      if (regex.global && index === regex.lastIndex) {
        // Regular expression just matched a string of length 0.
        return void this.handleInfiniteMatches()
      }

      index = match.index
      indices.push({ start: index, end: match.index + match[0].length })
    }

    // Convert matches from index pairs to pairs of Points.
    let points: { start: Point, end: Point }[] = []
    points = indices.map((pair) => {
      let startIndex  = pair.start
      let startPos    = this.document.posFromIndex(startIndex)
      let startCoords = this.instance.charCoords(startPos, CM_COORD_SYSTEM)
      let startPoint  = { index: startIndex, pos: startPos, coords: startCoords }

      let endIndex  = pair.end
      let endPos    = this.document.posFromIndex(endIndex)
      let endCoords = this.instance.charCoords(endPos, CM_COORD_SYSTEM)
      let endPoint  = { index: endIndex, pos: endPos, coords: endCoords }

      return { start: startPoint, end: endPoint }
    })

    return points
  }

  // Given a point pair, create a CodeMirror text marker over that span.
  private markTextWithPointPair (pair: PointPair): CodeMirror.TextMarker {
    return this.document.markText(pair.start.pos, pair.end.pos, {
      className: CM_HIGHLIGHT_CLASS,
    })
  }

  // Initialize event listeners for managing the lifecycle of a Grip drag.
  private handleDragStart (h: Highlight, isStart: boolean, offset: [number, number]) {
    let cursor: CodeMirror.Position = null

    const handleDragWrapper = ((event: MouseEvent) => {
      this.handleDrag(h, isStart, event.pageX, event.pageY)
    }).bind(this)

    const handleDragStopWrapper = ((event: MouseEvent) => {
      this.handleDragStop(cursor, event.pageX, event.pageY)
      util.offEvent(window.document.body, 'mousemove', handleDragWrapper)
    }).bind(this)

    this.isDragging = true
    this.hideAllPopovers()
    cursor = this.setReadOnly()
    util.onEvent(window.document.body, 'mousemove', handleDragWrapper)
    util.onceEvent(window.document.body, 'mouseup', handleDragStopWrapper)
  }

  // After a mouse movement, update the appropriate highlight with the new
  // position. Trigger a re-render if the new position is different than the
  // previous position.
  private handleDrag (h: Highlight, isStart: boolean, x: number, y: number) {
    // Compute the first position in the document this grip can legally occupy.
    let lowerBound = (isStart === false)
      ? util.getPositionAfter(this.document, h.getStart().pos)
      : (h.getPrev() === null)
        ? util.getFirstPosition(this.document)
        : h.getPrev().getEnd().pos

    // Compute the last position in the document this grip can legally occupy.
    let upperBound = (isStart)
      ? util.getPositionBefore(this.document, h.getEnd().pos)
      : (h.getNext() === null)
        ? util.getLastPosition(this.document)
        : h.getNext().getStart().pos

    // Compute the closest position to the given X/Y coordinates.
    let newPos = this.instance.coordsChar({ left: x, top: y }, 'page')

    // If the computed position is not between the lower & upperbounds, assign
    // the closest bound to be the new position instead.
    if (util.lessThanPosition(newPos, lowerBound)) {
      newPos = lowerBound
    } else if (util.greaterThanPosition(newPos, upperBound)) {
      newPos = upperBound
    }

    // Only trigger a re-draw if the new position would be different than the
    // grip's current position.
    let oldPos = isStart ? h.getStart().pos : h.getEnd().pos

    if (util.samePosition(oldPos, newPos) === false) {
      let index  = this.document.indexFromPos(newPos)
      let coords = this.instance.charCoords(newPos, CM_COORD_SYSTEM)

      if (isStart) {
        h.setStart({ index: index, pos: newPos, coords: coords })
      } else {
        h.setEnd({ index: index, pos: newPos, coords: coords })
      }

      this.clearMarkers()
      this.drawMarkers()
      this.attachMarkerListeners()
      this.forceUpdate()
    }
  }

  private handleDragStop (cursor: CodeMirror.Position, x: number, y: number) {
    this.isDragging = false
    this.unsetReadOnly(cursor)
  }

  private setReadOnly (): CodeMirror.Position {
    let cursor: CodeMirror.Position = null

    if (this.instance.hasFocus()) {
      cursor = this.document.getCursor()
    }

    this.instance.setOption('readOnly', 'nocursor')
    return cursor
  }

  private unsetReadOnly (cursor: CodeMirror.Position) {
    this.instance.setOption('readOnly', false)

    if (cursor !== null) {
      this.instance.focus()
      this.document.setCursor(cursor)
    }
  }

  // Have each highlight produce Grip elements corresponding to that highlight's
  // left and right endpoints.
  private collectGrips (): ReactNode[] {
    if (this.highlights) {
      return this.highlights.reduce((grips, h) => {
        grips.push(<StartGrip
          key={grips.length}
          point={h.getStart()}
          onDragStart={this.handleDragStart.bind(this, h, true)} />)

        grips.push(<EndGrip
          key={grips.length}
          point={h.getEnd()}
          onDragStart={this.handleDragStart.bind(this, h, false)} />)

        return grips
      }, [])
    }

    return []
  }

  render () {
    return (
      <div className="corpus-editor">
        <Overlay>
          {this.popovers}
          {this.collectGrips()}
        </Overlay>
        <textarea ref={(input) => { this.textarea = input }} />
      </div>
    )
  }
}
