//
// src/main/typescript/corpus-editor.ts
// RegEx Frontend
//
// Created on 2/20/17
//

import 'codemirror'
import { Position } from 'codemirror'
import { RegionList } from './region-list'
import { Region } from './region'
import { Point } from './point'
import * as util from './util'

const POPOVER_WIDTH = 32

export class CorpusEditor {
  wrapper: HTMLElement
  grips: HTMLElement
  cm: CodeMirror.Editor
  doc: CodeMirror.Doc
  regex: RegExp
  regions: RegionList = new RegionList()
  textMarkers: [CodeMirror.TextMarker, Region, HTMLElement][] = []
  palette: string[] = []
  nextColor: number = 0
  offset: { top: number, left: number }
  isRegionCleared: boolean = true
  popoverTimeout: number

  onInfiniteMatches: () => void = util.noop
  onMatches: (totalMatches: number) => void = util.noop

  constructor (wrapper: HTMLElement, palette: string[] = []) {
    this.wrapper = wrapper

    // Create element to hold <Grip> elements.
    this.grips = util.createElement('div', this.wrapper)
    util.addClass(this.grips, 'grips')

    // Create element to hold CodeMirror editor.
    let editorElem = util.createElement('div', this.wrapper)
    util.addClass(editorElem, 'editor')
    util.addClass(editorElem, 'editable-font')

    // Create a CodeMirror instance.
    this.cm = window['CodeMirror'](editorElem, {
      lineWrapping: true,
      flattenSpans: false,
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
    let boundingRect = this.wrapper.getBoundingClientRect()
    this.offset = { top: boundingRect.top, left: boundingRect.left }
  }

  getValue (): string {
    return this.doc.getValue()
  }

  setValue (value: string) {
    this.clearRegions()
    this.doc.setValue(value)
  }

  createTextMarker (reg: Region, color: string): CodeMirror.TextMarker {
    const opts = {
      className: 'corpus-match-marker',
      css: 'background-color:' + color,
    }

    let start = reg.start.pos
    let end = { line: reg.end.pos.line, ch: reg.end.pos.ch + 1}
    let marker = this.doc.markText(start, end, opts)

    return marker
  }

  remarkText () {
    this.hidePopover()
    this.clearTextMarkers()
    this.resetColor()

    let tuples: [CodeMirror.TextMarker, Region, HTMLElement][] = []
    this.regions.forEach((reg, i) => {
      let marker = this.createTextMarker(reg, this.getNextColor())
      tuples[i] = [marker, reg, null]
    })

    this.textMarkers = tuples
    this.attachMarkerListeners()
  }

  attachMarkerListeners () {
    setTimeout(() => {
      let markerElements = document.querySelectorAll('span.corpus-match-marker')
      for (let i = 0; i < markerElements.length; i++) {
        this.textMarkers[i][2] = markerElements[i] as HTMLElement
      }

      this.textMarkers.forEach((tuple) => {
        let [marker, reg, elem] = tuple

        elem.addEventListener('mouseover', () => {
          this.showMatchPopover(reg, elem)
        })

        elem.addEventListener('mouseout', this.delayHidePopover.bind(this))
      })
    }, 0)
  }

  clearTextMarkers () {
    this.textMarkers.forEach((tuple) => tuple[0].clear())
    this.textMarkers = []
  }

  showMatchPopover (reg: Region, elem: HTMLElement) {
    this.hidePopover()

    let gripsBox = this.grips.getBoundingClientRect()
    let elemBox = elem.getBoundingClientRect()

    // Create & position popover element.
    let popoverElem = util.createElement('div', this.grips)
    util.addClass(popoverElem, 'match-popover')

    let deleteBtn = util.createElement('button', popoverElem)
    util.addClass(deleteBtn, 'action')
    util.setAttr(deleteBtn, 'data-color', 'red')
    util.setText(deleteBtn, '\u2717')
    deleteBtn.addEventListener('click', () => {
      this.removeRegion(reg)
    })

    let left = ((elemBox.left - gripsBox.left) + (elemBox.width / 2)) - (POPOVER_WIDTH / 2)
    util.setCSS(popoverElem, 'left', left)

    let top = elemBox.bottom - gripsBox.bottom
    util.setCSS(popoverElem, 'top', top)

    // Attach event listeners to popover element.
    popoverElem.addEventListener('mouseover', () => {
      this.cancelHidingPopover()
    })

    popoverElem.addEventListener('mouseout', this.delayHidePopover.bind(this))
  }

  cancelHidingPopover () {
    clearTimeout(this.popoverTimeout)
  }

  hidePopover () {
    this.cancelHidingPopover()
    let popovers = this.grips.querySelectorAll('.match-popover')
    for (let i = 0; i < popovers.length; i++) {
      popovers[i].remove()
    }
  }

  delayHidePopover () {
    this.popoverTimeout = setTimeout(this.hidePopover.bind(this), 500)
  }

  addRegion (start: Point, end: Point) {
    let color = this.getNextColor()
    let reg = new Region(this, start, end, color)
    this.regions.insert(reg)

    reg.onMove = (left: Point, right: Point) => {
      this.remarkText()
    }

    this.isRegionCleared = false
  }

  removeRegion (reg: Region) {
    reg.remove()
    this.remarkText()
    this.onMatches(this.regions.length())
  }

  setRegex (regex: RegExp) {
    this.regex = regex
    this.clearRegions()

    if (regex instanceof RegExp) {
      this.findMatches()
    }
  }

  clearRegex () {
    this.regex = null
  }

  clearRegions () {
    if (this.isRegionCleared === false) {
      this.isRegionCleared = true
      this.regions.forEach((reg) => reg.remove())
      this.clearTextMarkers()
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

      let startIndex = match.index
      let startPos = this.doc.posFromIndex(startIndex)
      let startCoords = this.cm.charCoords(startPos, 'local')
      let start = { index: startIndex, pos: startPos, coords: startCoords }

      let endIndex = match.index + match[0].length - 1
      let endPos = this.doc.posFromIndex(endIndex)
      let endCoords = this.cm.charCoords(endPos, 'local')
      let end = { index: endIndex, pos: endPos, coords: endCoords }

      this.addRegion(start, end)
      totalMatches++

      if (!this.regex.global) {
        break
      }
    }

    this.remarkText()
    this.onMatches(totalMatches)
  }

  resetColor () {
    this.nextColor = 0
  }

  getNextColor (): string {
    if (this.nextColor >= this.palette.length) {
      this.nextColor = 0
    }

    return this.palette[this.nextColor++] || '#8bc4ea'
  }
}
