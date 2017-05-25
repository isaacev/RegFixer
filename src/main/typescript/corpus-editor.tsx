//
// src/main/typescript/corpus-editor.tsx
// RegEx Frontend
//
// Created on 4/7/17
//

import 'codemirror'
import { Component, ReactNode, MouseEvent } from 'react'
import { Overlay } from './overlay'
import { Underlay } from './underlay'
import { StartGrip, EndGrip } from './grip'
import { Popover } from './popover'
import { MouseoverZone } from './mouseover-zone'
import { MouseoverField } from './mouseover-field'
import { Button } from './button'
import { PointPair, Point } from './point'
import { Highlight } from './highlight'
import { HighlightList } from './highlight-list'
import * as util from './util'

const CM_COORD_SYSTEM = 'local'

interface Props {
  regex: string
  corpus: string
  onCorpusChange: (newCorpus: string) => void
  onMatchesChange: (matches: { start: number, end: number }[]) => void
  onEmptyRegex: () => void
  onInfiniteMatches: () => void
  onBrokenRegex: () => void
}

interface State {
  regex: string
}

export class CorpusEditor extends Component<Props, State> {
  private root: HTMLDivElement
  private textarea: HTMLTextAreaElement
  private instance: CodeMirror.Editor
  private document: CodeMirror.Doc
  private highlights: HighlightList
  private isDragging: boolean = false
  private popover: ReactNode = null
  private popoverTimeout: number

  private mouseoverField: MouseoverField = new MouseoverField()

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

  componentWillUpdate () {
    this.mouseoverField.clearHighlightZones()
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
    this.props.onCorpusChange(this.instance.getValue())
  }

  private handleCursorActivity () {
    this.mouseoverField.clearSelectionZones()
    this.hideAllPopovers()

    if (this.document.somethingSelected()) {
      this.document.listSelections().forEach((selection) => {
        let startPos = selection.anchor
        let endPos = selection.head

        if (util.lessThanPosition(endPos, startPos)) {
          startPos = selection.head
          endPos = selection.anchor
        }

        let startIndex = this.document.indexFromPos(startPos)
        let startCoords = this.instance.charCoords(startPos, CM_COORD_SYSTEM)
        let startPoint = { index: startIndex, pos: startPos, coords: startCoords }

        let endIndex = this.document.indexFromPos(endPos)
        let endCoords = this.instance.charCoords(endPos, CM_COORD_SYSTEM)
        let endPoint = { index: endIndex, pos: endPos, coords: endCoords }

        let pair: PointPair = { start: startPoint, end: endPoint }

        let x = startCoords.left
        let y = startCoords.top
        let w = endCoords.left - x
        let h = endCoords.bottom - y
        let zone = new MouseoverZone(x, y, w, h)

        zone.on('over', () => {
          zone.on('over', this.showAddPopover.bind(this, pair, () => {
            this.document.setCursor(this.document.getCursor())
          }))
          zone.on('out', this.delayHideAllPopovers.bind(this))
          this.mouseoverField.addZone('highlight', zone)
        })

        this.mouseoverField.addZone('selection', zone)
      })
    }
  }

  private handleMouseActivity (event: MouseEvent<HTMLDivElement>) {
    let x = event.clientX - this.root.offsetLeft
    let y = event.clientY - this.root.offsetTop

    if (event.type === 'mousemove') {
      this.mouseoverField.trigger('move', x, y)
    } else if (event.type === 'mouseout') {
      this.mouseoverField.trigger('out', x, y)
    }
  }

  private handleNewPopoverZone (zone: MouseoverZone, h: Highlight): void {
    zone.on('over', this.showRemovePopover.bind(this, h))
    zone.on('out', this.delayHideAllPopovers.bind(this))
    this.mouseoverField.addZone('highlight', zone)
  }

  private showPopover (pair: PointPair, child: ReactNode): void {
    if (this.isDragging === false) {
      let cancelHideAllPopovers = this.cancelHideAllPopovers.bind(this)
      let delayHideAllPopovers = this.delayHideAllPopovers.bind(this)
      this.cancelHideAllPopovers()

      this.popover = (
        <Popover
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
    setTimeout(this.cancelHideAllPopovers.bind(this), 0)
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

  private showAddPopover (pair: PointPair, callback: () => void) {
    this.showPopover(pair, (
      <Button
        glyph="\u2713"
        color="green"
        arrow={true}
        onClick={() => {
          this.hideAllPopovers()
          this.addHighlight(pair)
          callback.apply(this, [])
        }} />
    ))
  }

  private delayHideAllPopovers () {
    this.cancelHideAllPopovers()
    this.popoverTimeout = setTimeout(this.hideAllPopovers.bind(this), 500)
  }

  private cancelHideAllPopovers () {
    clearTimeout(this.popoverTimeout)
  }

  private hideAllPopovers () {
    this.popover = null
    this.cancelHideAllPopovers()
    this.forceUpdate()
  }

  private removeHighlight (h: Highlight) {
    if (this.highlights) {
      this.highlights.remove(h)
      this.props.onMatchesChange(this.highlights.getMatches())
      this.forceUpdate()
    }
  }

  private addHighlight (pair: PointPair): void {
    if (!this.highlights) {
      this.highlights = new HighlightList()
    }

    let highlight = new Highlight(pair)

    try {
      this.highlights.insert(highlight)
    } catch (err) {
      alert('matches cannot overlap')
      return
    }

    this.props.onMatchesChange(this.highlights.getMatches())
    this.forceUpdate()
  }

  // Removes text markers and data structure tracking highlights in corpus.
  private clearHighlights (): void {
    this.highlights = new HighlightList()
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
        let highlight = new Highlight(pair)
        return list.insert(highlight)
      }, new HighlightList())

      // Listen for mouse-over events on the text marker elements.
      this.props.onMatchesChange(this.highlights.getMatches())
    }

    this.forceUpdate()
  }

  private handleEmptyRegex (): void {
    this.props.onEmptyRegex()
  }

  private handleInfiniteMatches (): void {
    this.props.onInfiniteMatches()
  }

  private handleBrokenRegex (): void {
    this.props.onBrokenRegex()
  }

  // Runs a regular expression over the corpus and generates a list of pairs of
  // coordinates where each pair corresponds to a region of the corpus that
  // matched the regular expression. Causes no side effects.
  private getMatchingPointPairs (): PointPair[] | undefined {
    let regex: RegExp

    if (this.state.regex === '') {
      return void this.handleEmptyRegex()
    }

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

  // Initialize event listeners for managing the lifecycle of a Grip drag.
  private handleDragStart (h: Highlight, isStart: boolean, offset: [number, number]) {
    let cursor: CodeMirror.Position = null

    const handleDragWrapper = ((event: MouseEvent<HTMLDivElement>) => {
      this.handleDrag(h, isStart, event.pageX, event.pageY)
    }).bind(this)

    const handleDragStopWrapper = ((event: MouseEvent<HTMLDivElement>) => {
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
    let handleMouseActivity = this.handleMouseActivity.bind(this)
    let handleNewPopoverZone = this.handleNewPopoverZone.bind(this)

    return (
      <div
        className="corpus-editor"
        ref={(input) => { this.root = input}}
        onMouseMove={handleMouseActivity}
        onMouseOut={handleMouseActivity}>
        <Overlay>
          {this.popover}
          {this.collectGrips()}
        </Overlay>
        <textarea ref={(input) => { this.textarea = input }} />
        <Underlay
          highlightList={this.highlights}
          onNewPopoverZone={handleNewPopoverZone} />
      </div>
    )
  }
}
