//
// src/query-editor.ts
// RegEx Frontend
//
// Created on 2/20/17
//

import CodeMirror from 'codemirror'
import 'codemirror-no-newlines'
import * as util from './util'

const firstDecorator = '/'
const lastDecorator = '/'

export class QueryEditor {
  wrapper: HTMLElement
  cm: CodeMirror.Editor
  doc: CodeMirror.Doc
  onChange: (boolean, RegExp) => void = function () {}
  bubble: HTMLElement

  constructor (wrapper: HTMLElement) {
    this.wrapper = wrapper

    // Create element to hold an instance of the CodeMirror editor.
    let regexElem = util.createElement('div', this.wrapper)
    util.addClass(regexElem, 'regex')
    util.addClass(regexElem, 'editable-font')

    // Create a CodeMirror instance.
    let decorators = firstDecorator + lastDecorator
    this.cm = CodeMirror(regexElem, { value: decorators })
    this.cm.setOption('scrollbarStyle', 'null')
    this.cm.setOption('noNewlines', true)
    this.doc = this.cm.getDoc()

    this.setValue('')

    // Listen for changes to the regular expression contents.
    this.cm.on('change', () => {
      let regex = this.getRegex()

      if (typeof this.onChange === 'function') {
        this.onChange(regex === null, regex)
      }
    })

    // Create a bubble element.
    this.bubble = util.createElement('div', this.wrapper)
    util.addClass(this.bubble, 'bubble')

    // Hide bubble element by default.
    this.hideBubble()
  }

  getValue (): string {
    let raw = this.doc.getValue()
    let trimmed = raw.slice(
      firstDecorator.length,
      raw.length - lastDecorator.length
    )

    return trimmed
  }

  setValue (val: string) {
    // Add a decorator to the beginning of the regular expression.
    let firstDecStart = {line: 0, ch: 0}
    let firstDecEnd = {line: 0, ch: firstDecorator.length}
    this.doc.markText(firstDecStart, firstDecEnd, {
      className: 'regex-decorator',
      atomic: true,
      readOnly: true,
      inclusiveLeft: true
    })

    // Add a decorator to the end of the regular expression.
    let lastDecStart = {line: 0, ch: firstDecEnd.ch}
    let lastDecEnd = {line: 0, ch: lastDecStart.ch + lastDecorator.length}
    this.doc.markText(lastDecStart, lastDecEnd, {
      className: 'regex-decorator',
      atomic: true,
      readOnly: true,
      inclusiveRight: true
    })

    this.doc.replaceRange(val, firstDecEnd, lastDecStart)
  }

  getRegex (): RegExp {
    let trimmed = this.getValue()

    if (trimmed.length === 0) {
      return null
    }

    try {
      return new RegExp(trimmed, 'g')
    } catch (err) {
      return null
    }
  }

  setBubble (msg: string, status: BubbleStatus = BubbleStatus.normal) {
    util.setAttr(this.bubble, 'data-status', BubbleStatus[status])
    util.setText(this.bubble, msg)
  }

  hideBubble () {
    util.setAttr(this.bubble, 'data-status', BubbleStatus[BubbleStatus.hidden])
  }
}

export enum BubbleStatus {
  hidden = 0,
  normal,
  red,
}
