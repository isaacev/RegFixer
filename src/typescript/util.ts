//
// src/typescript/util.ts
// RegEx Frontend
//
// Created on 2/20/17
//

import CodeMirror from 'codemirror'
import { Point } from './point'

export const charWidth = 14.4
export const charHeight = 28

export function samePoint (a: Point, b: Point): boolean {
  return (a.line === b.line && a.ch === b.ch)
}

export function lessThanPoint (a: Point, b: Point): boolean {
  if (a.line === b.line) { return a.ch < b.ch }
  return a.line < b.line
}

export function greaterThanPoint (a: Point, b: Point): boolean {
  if (a.line === b.line) { return a.ch > b.ch }
  return a.line > b.line
}

export function clampPoint (val: Point, min: Point, max: Point): Point {
  if (val.line === min.line && val.ch < min.ch) { return min }
  if (val.line === max.line && val.ch > max.ch) { return max }
  if (val.line < min.line) { return min }
  if (val.line > max.line) { return max }
  return val
}

export function getFirstPoint (doc: CodeMirror.Doc): Point {
  let firstLine = doc.firstLine()

  return { line: firstLine, ch: 0 }
}

export function getLastPoint (doc: CodeMirror.Doc): Point {
  let lastLine = doc.lastLine()
  let lastCh = doc.getLine(lastLine).length

  return { line: lastLine, ch: lastCh }
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
  let naiveCoords = cm.charCoords(point, 'local')

  // If CodeMirror describes a character coordinate as having a width of 0,
  // check if the character coordinate represents the position of a newline
  // character.
  if (naiveCoords.right - naiveCoords.left === 0) {
    let doc = cm.getDoc()
    let lastLineNum = doc.lastLine()

    // If the point is from the last line of the document then it can't end in
    // a newline so further checks can be skipped.
    if (point.line >= lastLineNum) {
      return naiveCoords
    }

    let lineLength = doc.getLine(point.line).length
    if (point.ch === lineLength) {
      // Point corresponds to a newline.
      let charWidth = cm.defaultCharWidth()
      let newlineCoords = naiveCoords
      newlineCoords.right = naiveCoords.left + charWidth

      return newlineCoords
    }
  }

  return naiveCoords
}
