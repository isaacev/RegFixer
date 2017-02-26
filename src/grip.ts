//
// src/grip.ts
// RegEx Frontend
//
// Created on 2/20/17
//

import CodeMirror from 'codemirror'
import { Region } from './region'
import { Point } from './point'
import * as util from './util'

class Grip {
  parent: Region
  index: Point
  elem: HTMLElement

  constructor (parent: Region, index: Point, topOffset: number, color: string) {
    this.parent = parent
    this.index = index

    this.elem = util.createElement('div')
    util.addClass(this.elem, 'grip')
    util.setCSS(this.elem, 'top', topOffset)
    util.setCSS(this.elem, 'background-color', color)
    util.onEvent(this.elem, 'mousedown', (event: MouseEvent) => {
      // Remove editing control from the editor when a drag begins.
      let prevCursorPos = this.parent.editor.doc.getCursor()
      let wasFocused = this.parent.editor.cm.hasFocus()
      this.parent.editor.cm.setOption('readOnly', 'nocursor')

      // Prevent mouse-down event from propogating to any other elements.
      event.stopPropagation()
      event.preventDefault()

      // Compute last point once at the start of each drag so that it doesn't
      // need to be re-calculated each mouse movement.
      let firstPoint = util.getFirstPoint(this.parent.editor.doc)
      let lastPoint = util.prevChPoint(util.getLastPoint(this.parent.editor.doc))

      // Store mouse move function in its own variable so that it can be
      // easily attached and detached from the window's mouse events.
      let onMouseMove = this.onMouseMove.bind(this, [firstPoint, lastPoint])

      let onWindowMouseUp = () => {
        // Return editing control to editor once the drag has completed.
        this.parent.editor.cm.setOption('readOnly', false)

        if (wasFocused) {
          this.parent.editor.doc.setCursor(prevCursorPos)
          this.parent.editor.cm.focus()
        }

        util.offEvent(window.document.body, 'mousemove', onMouseMove)
        util.removeClass(window.document.body, 'grabbing-cursor')
      }

      util.addClass(window.document.body, 'grabbing-cursor')
      util.onceEvent(window.document.body, 'mouseup', onWindowMouseUp)
      util.onEvent(window.document.body, 'mousemove', onMouseMove)
    })

    this.parent.editor.grips.appendChild(this.elem)
    this.updatePosition()
  }

  updatePosition () {
    let left = (this.index.ch * util.charWidth) - 4
    let top = (this.index.line * util.charHeight) - 4
    util.setCSS(this.elem, 'left', left)
    util.setCSS(this.elem, 'top', top)
  }

  onMouseMove (docLimits: [Point, Point], event: MouseEvent) {
    throw new Error('unsupported mouse movement event')
  }

  remove () {
    util.removeElement(this.elem)
  }
}

export class LeftGrip extends Grip {
  constructor (parent: Region, index: Point, color: string) {
    super(parent, index, -4, color)
  }

  onMouseMove (docLimits: [Point, Point], event: MouseEvent) {
    // Calculate the mouse position on screen relative to the upper left-hand
    // corner of the editing element.
    let editorX = this.parent.editor.offset.left
    let editorY = this.parent.editor.offset.top
    let gripX = event.clientX - editorX
    let gripY = event.clientY - editorY

    // The lowest index in the document as limited by either an earlier matched
    // region or by the start of the document.
    let leftmostBound = this.parent.link.prev
      ? util.nextChPoint(this.parent.link.prev.value.end)
      : docLimits[0]

    // The highest index in the document as limited by the end of this region.
    let rightmostBound = this.parent.end

    // A legal point in the document that is closest to the mouse's position
    // and that satisfies limits on the length of the lines and restrictions
    // on how far the grip can be moved forward and backward in the document.
    let gripPoint = pxToLegalPoint(
      this.parent.editor.doc,
      gripX, gripY,
      leftmostBound, rightmostBound)

    if (util.samePoint(gripPoint, this.index) === false) {
      this.parent.start = gripPoint
    }
  }
}

export class RightGrip extends Grip {
  constructor (parent: Region, index: Point, color: string) {
    super(parent, index, util.charHeight - 4, color)
  }

  // updatePosition is different for RightGrip because the grip is on the right
  // side of its respective character column instead of on the left side as is
  // the case with LeftGrips (and is the default position defined by Grip)
  updatePosition () {
    let left = ((this.index.ch + 1) * util.charWidth) - 4
    let top = ((this.index.line + 1) * util.charHeight) - 4
    util.setCSS(this.elem, 'left', left)
    util.setCSS(this.elem, 'top', top)
  }

  onMouseMove (docLimits: [Point, Point], event: MouseEvent) {
    // Calculate the mouse position on screen relative to the upper left-hand
    // corner of the editing element.
    let editorX = this.parent.editor.offset.left
    let editorY = this.parent.editor.offset.top
    let gripX = event.clientX - editorX - util.charWidth
    let gripY = event.clientY - editorY - util.charHeight

    // The highest index in the document as limited by either a later matched
    // region or by the end of the document.
    let rightmostBound = this.parent.link.next
      ? util.prevChPoint(this.parent.link.next.value.start)
      : docLimits[1]

    // The lowest index as limited by the start of this region
    let leftmostBound = this.parent.start

    // A legal point in the document that is closest to the mouse's position
    // and that satisfies limits on the length of the lines and restrictions
    // on how far the grip can be moved forward and backward in the document.
    let gripPoint = pxToLegalPoint(
      this.parent.editor.doc,
      gripX, gripY,
      leftmostBound, rightmostBound)

    if (util.samePoint(gripPoint, this.index) === false) {
      this.parent.end = gripPoint
    }
  }
}

function pxToLegalPoint (doc: CodeMirror.Doc, x: number, y: number, left: Point, right: Point): Point {
  // Set x and y to 0 if negaive.
  x = (x < 0) ? 0 : x
  y = (y < 0) ? 0 : y

  // The line/column point assuming the editing grid is infinite and without
  // constraints from line endings or other matching regions.
  let naivePoint = {
    line: Math.round(y / util.charHeight),
    ch: Math.round(x / util.charWidth)
  }

  // Limit point so that it cant extend past the length of the line it is on.
  let lastLine = doc.lastLine()
  let lineNum = Math.min(naivePoint.line, lastLine)
  let lineLength = doc.getLine(lineNum).length
  let lineRestrictedPoint = {
    line: (lastLine < naivePoint.line) ? lastLine : naivePoint.line,
    ch: (lineLength < naivePoint.ch) ? lineLength : naivePoint.ch
  }

  // Limit point so that it cant extend into another matching region that
  // comes before or after.
  let neighborRestrictedPoint = lineRestrictedPoint
  if (util.lessThanPoint(lineRestrictedPoint, left)) {
    neighborRestrictedPoint = left
  } else if (util.greaterThanPoint(lineRestrictedPoint, right)) {
    neighborRestrictedPoint = right
  }

  return neighborRestrictedPoint
}
