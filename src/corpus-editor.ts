//
// src/corpus-editor.ts
// RegEx Frontend
//
// Created on 2/20/17
//

import CodeMirror from 'codemirror'
import { InfiniteMatchesError } from './errors'
import { RegionList } from './region-list'
import { Region } from './region'
import { Point } from './point'
import * as util from './util'

export class CorpusEditor {
  wrapper: HTMLElement
  grips: HTMLElement
  canvas: HTMLElement
  context: CanvasRenderingContext2D
  cm: CodeMirror.Editor
  doc: CodeMirror.Doc
  regions: RegionList = new RegionList()
  palette: string[] = []
  nextColor: number = 0
  offset: { top: number, left: number }

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
    this.cm = CodeMirror(editorElem, { value: 'foo bar baz' })
    this.doc = this.cm.getDoc()

    // Store a list of alternating colors used to differentiate adjacent
    // matches. If no colors are provided, gray will be used for every match.
    this.palette = palette

    // Cache editor position on the page.
    let boundingRect = this.canvas.getBoundingClientRect()
    this.offset = { top: boundingRect.top, left: boundingRect.left }
  }

  setValue (value: string) {
    this.clearRegions()
    this.doc.setValue(value)
  }

  drawCanvas () {
    this.context.clearRect(0, 0, this.canvas.offsetWidth, this.canvas.offsetHeight)

    this.regions.forEach((reg) => {
      this.context.fillStyle = reg.color
      let nw = {
        x: Math.round(reg.start.ch * util.charWidth),
        y: Math.round(reg.start.line * util.charHeight)
      }
      let se = {
        x: Math.round((reg.end.ch + 1) * util.charWidth),
        y: Math.round((reg.end.line + 1) * util.charHeight)
      }

      if (reg.start.line === reg.end.line) {
        // Convert indices to (x,y) coordinates.
        let width = se.x - nw.x
        let height = util.charHeight

        // Draw region rectangle on the canvas.
        this.context.fillRect(nw.x, nw.y, width, height)
      } else {
        // 1. Draw from left grip to end of line
        let lineLength = this.doc.getLine(reg.start.line).length
        let width = (lineLength - reg.start.ch + 1) * util.charWidth
        this.context.fillRect(nw.x, nw.y, width, util.charHeight)

        // 2. Draw fully convered middle lines
        for (let i = 1, l = reg.end.line - reg.start.line; i < l; i++) {
          let y = nw.y + (util.charHeight * i)
          let lineLength = this.doc.getLine(reg.start.line + i).length
          let width = (lineLength + 1) * util.charWidth
          this.context.fillRect(0, y, width, util.charHeight)
        }

        // 3. Draw from start of line to right grip
        let y = (reg.end.line) * util.charHeight
        width = (reg.end.ch + 1) * util.charWidth
        this.context.fillRect(0, y, width, util.charHeight)
      }
    })
  }

  addRegion (start: Point | number, end: Point | number) {
    if (typeof start === 'number') {
      start = this.doc.posFromIndex(start)
    }

    if (typeof end === 'number') {
      end = this.doc.posFromIndex(end - 1)
    }

    let color = this.getNextColor()
    let reg = new Region(this, start, end, color)
    this.regions.insert(reg)
    this.drawCanvas()
  }

  clearRegions () {
    this.regions.forEach((reg) => reg.remove())
    this.drawCanvas()
    this.resetColor()
  }

  findMatches (regex: RegExp): number {
    let corpus = this.doc.getValue()
    let error = null
    let index = null
    let totalMatches = 0

    while (!error) {
      let match = regex.exec(corpus)

      if (!match) {
        break
      }

      if (regex.global && index === regex.lastIndex) {
        throw new InfiniteMatchesError()
      }

      index = match.index
      this.addRegion(match.index, match.index + match[0].length)
      totalMatches++

      if (!regex.global) {
        break
      }
    }

    return totalMatches
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
