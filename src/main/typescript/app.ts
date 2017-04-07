//
// src/main/typescript/app.ts
// RegEx Frontend
//
// Created on 2/20/17
//

import * as React from 'react'
import * as ReactDOM from 'react-dom'
import { QueryEditor } from './query-editor'
import { CorpusEditor } from './corpus-editor'
import { createWrapper } from './util'

export class App {
  private appWrapper: HTMLElement
  private queryWrapper: HTMLElement
  private corpusWrapper: HTMLElement
  private queryEditor: QueryEditor
  private corpusEditor: CorpusEditor

  constructor (appWrapper: HTMLElement) {
    // Create basic app skeleton.
    this.appWrapper = appWrapper
    this.queryWrapper = createWrapper(appWrapper, 'query')
    this.corpusWrapper = createWrapper(appWrapper, 'corpus')

    // Initialize editors.
    let firstQuery = '\\w'
    this.initQueryEditor(firstQuery)
    this.initCorpusEditor(firstQuery)
  }

  private whenTheQueryChanges (newQuery: string) {
    this.corpusEditor.setRegex(this.stringToRegex(newQuery))
  }

  private whenTheQueryIsBroken () {
    this.updateQueryEditor(this.queryEditor.props.query, 0)
  }

  private whenTheMatchesChange (newTotalMatches: number) {
    this.updateQueryEditor(this.queryEditor.props.query, newTotalMatches)
  }

  private initQueryEditor (firstQuery: string) {
    this.updateQueryEditor(firstQuery, 0)
  }

  private updateQueryEditor (newQuery: string, newTotalMatches: number) {
    let elem = React.createElement(QueryEditor, {
      query: newQuery,
      totalMatches: newTotalMatches,
      onChange: this.whenTheQueryChanges.bind(this),
    }, [])
    this.queryEditor = ReactDOM.render(elem, this.queryWrapper)
  }

  private initCorpusEditor (firstQuery: string) {
    this.corpusEditor = new CorpusEditor(this.corpusWrapper, [])
    this.corpusEditor.setValue('abc def 123 456 ghi')
    this.corpusEditor.onMatches = this.whenTheMatchesChange.bind(this)
    this.corpusEditor.onInfiniteMatches = this.whenTheMatchesChange.bind(this, Infinity)
    this.corpusEditor.setRegex(this.stringToRegex(firstQuery))
  }

  private stringToRegex (s: string): RegExp {
    try {
      return new RegExp(s, 'g')
    } catch (ex) {
      this.whenTheQueryIsBroken()
      return null
    }
  }
}
