//
// src/main/typescript/util.ts
// RegEx Frontend
//
// Created on 2/20/17
//

import 'codemirror'
import { Point } from './point'

export const charWidth = 14.4
export const charHeight = 28

export function samePoint (a: Point, b: Point): boolean {
  return (a.pos.line === b.pos.line && a.pos.ch === b.pos.ch)
}

export function lessThanPosition (a: CodeMirror.Position, b: CodeMirror.Position): boolean {
  if (a.line === b.line) { return a.ch < b.ch }
  return a.line < b.line
}

export function lessThanPoint (a: Point, b: Point): boolean {
  return lessThanPosition(a.pos, b.pos)
}

export function greaterThanPosition (a: CodeMirror.Position, b: CodeMirror.Position): boolean {
  if (a.line === b.line) { return a.ch > b.ch }
  return a.line > b.line
}

export function greaterThanPoint (a: Point, b: Point): boolean {
  return greaterThanPosition(a.pos, b.pos)
}

export function clampPoint (val: Point, min: Point, max: Point): Point {
  if (val.pos.line === min.pos.line && val.pos.ch < min.pos.ch) { return min }
  if (val.pos.line === max.pos.line && val.pos.ch > max.pos.ch) { return max }
  if (val.pos.line < min.pos.line) { return min }
  if (val.pos.line > max.pos.line) { return max }
  return val
}

export function getFirstPoint (doc: CodeMirror.Doc): Point {
  let firstLine = doc.firstLine()
  let firstCh = 0

  let pos = { line: firstLine, ch: firstCh }
  let index = doc.indexFromPos(pos)
  let coords = doc.getEditor().charCoords(pos, 'local')

  return { index: index, pos: pos, coords: coords }
}

export function getLastPoint (doc: CodeMirror.Doc): Position {
  let lastLine = doc.lastLine()
  let lastCh = doc.getLine(lastLine).length - 1

  let pos = { line: lastLine, ch: lastCh }
  let index = doc.indexFromPos(pos)
  let coords = doc.getEditor().charCoords(pos, 'local')

  return { index: index, pos: pos, coords: coords }
}

/**
 * DOM manipulation functions. Requires IE 11 or newer.
 */

export function createElement (tagName: string, parent: HTMLElement = null): HTMLElement {
  let elem = document.createElement(tagName)

  if (parent !== null) {
    parent.appendChild(elem)
  }

  return elem
}

export function removeElement (elem: HTMLElement) {
  let parent = elem.parentNode
  parent.removeChild(elem)
}

export function createWrapper (parent: HTMLElement, className: string): HTMLElement {
  let child = createElement('div', parent)
  addClass(child, className)

  return child
}

export function addClass (elem: HTMLElement, className: string) {
  elem.classList.add(className)
}

export function removeClass (elem: HTMLElement, className: string) {
  elem.classList.remove(className)
}

export function setAttr (elem: HTMLElement, attrName: string, val: string) {
  elem.setAttribute(attrName, val)
}

export function setText (elem: HTMLElement, val: string) {
  elem.innerText = val
}

export function setCSS (elem: HTMLElement, propName: string, val: any) {
  elem.style[propName] = val
}

export function onEvent (elem: HTMLElement, eventName: string, cb: (MouseEvent) => void) {
  elem.addEventListener(eventName, cb)
}

export function onceEvent (elem: HTMLElement, eventName: string, cb: (MouseEvent) => void) {
  let once = function (event: MouseEvent) {
    elem.removeEventListener(eventName, once)
    cb(event)
  }

  elem.addEventListener(eventName, once)
}

export function offEvent (elem: HTMLElement, eventName: string, cb: (MouseEvent) => void) {
  elem.removeEventListener(eventName, cb)
}

/**
 * Other util functions.
 */

export function debounce (fn: () => void, wait: number): () => void {
  let timeout: number

  return () => {
    let later = () => {
      timeout = null
      fn.apply(fn)
    }

    clearTimeout(timeout)
    timeout = setTimeout(later, wait)
  }
}

export function noop () {
  // Do nothing.
}

type CharCoords = { left: number, right: number, top: number, bottom: number }
export function charCoordsShowNewlines (cm: CodeMirror.Editor, point: Point): CharCoords {
  let naiveCoords = point.coords

  // If CodeMirror describes a character coordinate as having a width of 0,
  // check if the character coordinate represents the position of a newline
  // character.
  if (naiveCoords.right - naiveCoords.left === 0) {
    let doc = cm.getDoc()
    let lastLineNum = doc.lastLine()

    // If the position is from the last line of the document then it can't end in
    // a newline so further checks can be skipped.
    if (point.pos.line >= lastLineNum) {
      return naiveCoords
    }

    let lineLength = doc.getLine(position.line).length
    if (point.pos.ch === lineLength) {
      // Position corresponds to a newline.
      let charWidth = cm.defaultCharWidth()
      let newlineCoords = naiveCoords
      newlineCoords.right = naiveCoords.left + charWidth

      return newlineCoords
    }
  }

  return naiveCoords
}
