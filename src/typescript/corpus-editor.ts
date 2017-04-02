//
// src/typescript/corpus-editor.ts
// RegEx Frontend
//
// Created on 2/20/17
//

import CodeMirror, { Position } from 'codemirror'
import { RegionList } from './region-list'
import { Region } from './region'
import * as util from './util'

const EDITOR_PADDING = 4

export class CorpusEditor {
  wrapper: HTMLElement
  grips: HTMLElement
  canvas: HTMLElement
  context: CanvasRenderingContext2D
  cm: CodeMirror.Editor
  doc: CodeMirror.Doc
  regex: RegExp
  regions: RegionList = new RegionList()
  palette: string[] = []
  nextColor: number = 0
  offset: { top: number, left: number }
  isRegionCleared: boolean = true

  onInfiniteMatches: () => void = util.noop
  onMatches: (totalMatches: number) => void = util.noop

  constructor (wrapper: HTMLElement, palette: string[] = []) {
    this.wrapper = wrapper

    // Create element to hold <Grip> elements.
    this.grips = util.createElement('div', this.wrapper)
    util.addClass(this.grips, 'grips')

    // Create canvas for drawing match backgrounds.
    this.canvas = util.createElement('canvas', this.wrapper)
    util.addClass(this.canvas, 'canvas')
    util.setAttr(this.canvas, 'width', this.wrapper.offsetWidth.toString())
    util.setAttr(this.canvas, 'height', this.wrapper.offsetHeight.toString())

    // Create drawing context.
    this.context = (this.canvas as HTMLCanvasElement).getContext('2d')

    // Create element to hold CodeMirror editor.
    let editorElem = util.createElement('div', this.wrapper)
    util.addClass(editorElem, 'editor')
    util.addClass(editorElem, 'editable-font')

    // Create a CodeMirror instance.
    this.cm = CodeMirror(editorElem, {
      lineWrapping: true
    })
    this.doc = this.cm.getDoc()

    // Listen for changes to the CodeMirror contents so that matches can be
    // updated to reflect the changes.
    this.cm.on('change', () => {
      this.clearRegions()

      if (this.regex instanceof RegExp) {
        this.findMatches()
      }
    })

    // Store a list of alternating colors used to differentiate adjacent
    // matches. If no colors are provided, gray will be used for every match.
    this.palette = palette

    // Cache editor position on the page.
    let boundingRect = this.canvas.getBoundingClientRect()
    this.offset = { top: boundingRect.top, left: boundingRect.left }
  }

  getValue (): string {
    return this.doc.getValue()
  }

  setValue (value: string) {
    this.clearRegions()
    this.doc.setValue(value)
  }

  clearCanvas () {
    let canvasSize = this.canvas.getBoundingClientRect()
    this.context.clearRect(0, 0, canvasSize.width, canvasSize.height)
  }

  drawCanvas () {
    let canvasSize = this.canvas.getBoundingClientRect()
    this.clearCanvas()

    this.regions.forEach((reg) => {
      this.context.fillStyle = reg.color
      let nw = util.charCoordsShowNewlines(this.cm, reg.start)
      let se = util.charCoordsShowNewlines(this.cm, reg.end)

      let x: number, y: number, w: number, h: number
      if (nw.top === se.top) {
        // Start and end of region are horizontally aligned. (Does not wrap).
        x = nw.left
        y = nw.top
        w = se.right - nw.left
        h = se.bottom - se.top
        this.context.fillRect(x, y, w, h)
      } else {
        // 1. Draw from left grip to the end line or to line wrap.
        x = nw.left
        y = nw.top
        w = canvasSize.width - nw.left
        h = nw.bottom - nw.top
        this.context.fillRect(x, y, w, h)

        // 2. Draw full covered lines
        let middleLines = Math.round((se.top - nw.bottom) / util.charHeight)
        x = EDITOR_PADDING
        y = nw.bottom
        w = canvasSize.width - EDITOR_PADDING
        h = util.charHeight * middleLines
        this.context.fillRect(x, y, w, h)

        // 3. Draw from start of line to right grip.
        x = EDITOR_PADDING
        y = se.top
        w = se.left + util.charWidth - EDITOR_PADDING
        h = se.bottom - se.top
        this.context.fillRect(x, y, w, h)
      }
    })
  }

  addRegion (start: Position | number, end: Position | number) {
    if (typeof start === 'number') {
      start = this.doc.posFromIndex(start) as Position
    }

    if (typeof end === 'number') {
      end = this.doc.posFromIndex(end - 1) as Position
    }

    let color = this.getNextColor()
    let reg = new Region(this, start, end, color)
    this.regions.insert(reg)

    reg.onMove = (left: Position, right: Position) => {
      this.drawCanvas()
    }

    this.isRegionCleared = false
  }

  setRegex (regex: RegExp) {
    this.regex = regex
    this.clearRegions()
    this.findMatches()
  }

  clearRegex () {
    this.regex = null
  }

  clearRegions () {
    if (this.isRegionCleared === false) {
      this.isRegionCleared = true
      this.regions.forEach((reg) => reg.remove())
      this.clearCanvas()
      this.resetColor()
    }
  }

  findMatches () {
    let corpus = this.doc.getValue()
    let error = null
    let index = null
    let totalMatches = 0

    while (!error) {
      let match = this.regex.exec(corpus)

      if (!match) {
        break
      }

      if (this.regex.global && index === this.regex.lastIndex) {
        this.clearRegions()
        this.onInfiniteMatches()
        return
      }

      index = match.index
      this.addRegion(match.index, match.index + match[0].length)
      totalMatches++

      if (!this.regex.global) {
        break
      }
    }

    this.drawCanvas()
    this.onMatches(totalMatches)
  }

  resetColor () {
    this.nextColor = 0
  }

  getNextColor (): string {
    if (this.nextColor >= this.palette.length) {
      this.nextColor = 0
    }

    return this.palette[this.nextColor++] || 'gray'
  }
}
