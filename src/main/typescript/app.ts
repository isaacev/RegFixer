//
// src/main/typescript/app.ts
// RegEx Frontend
//
// Created on 2/20/17
//

import * as React from 'react'
import * as ReactDOM from 'react-dom'
import * as request from 'superagent'
import { QueryEditor } from './query-editor'
import { CorpusEditor } from './corpus-editor'
import { createWrapper } from './util'

export class App {
  private appWrapper: HTMLElement
  private queryWrapper: HTMLElement
  private corpusWrapper: HTMLElement
  private queryEditor: QueryEditor
  private corpusEditor: CorpusEditor
  private query: string

  constructor (appWrapper: HTMLElement) {
    // Create basic app skeleton.
    this.appWrapper = appWrapper
    this.queryWrapper = createWrapper(appWrapper, 'query')
    this.corpusWrapper = createWrapper(appWrapper, 'corpus')

    // Initialize editors.
    this.query = '\\w+'
    this.initQueryEditor(this.query)
    this.initCorpusEditor(this.query)
  }

  private whenTheQueryChanges (newQuery: string) {
    this.query = newQuery
    this.corpusEditor.setRegex(this.stringToRegex(newQuery))
  }

  private whenTheSuggestionIsAccepted (newQuery: string) {
    this.query = newQuery
    this.updateQueryEditor(newQuery, 0)
    this.corpusEditor.setRegex(this.stringToRegex(newQuery))
  }

  private whenTheSuggestionIsRejected () {
    let oldQuery = this.query
    let oldMatches = this.queryEditor.props.totalMatches
    this.updateQueryEditor(oldQuery, oldMatches)
  }

  private whenTheQueryIsBroken () {
    this.updateQueryEditor(this.query, 0)
  }

  private whenTheMatchesChange (newTotalMatches: number) {
    this.updateQueryEditor(this.query, newTotalMatches)
  }

  private whenUserRequestsFix (oldQuery: string) {
    let corpus = this.corpusEditor.getValue()
    let matches = this.corpusEditor.getMatches()

    request
      .post('/api/fix')
      .send({ regex: oldQuery, ranges: matches, corpus: corpus })
      .end((err, res) => {
        if (err != null || res.status !== 200) {
          console.error(err)
          return
        }

        this.displaySuggestion(res.text.replace(/\"/g, '').replace(/\\\\/g, '\\'))
      })
  }

  private displaySuggestion (suggestion: string) {
    this.updateQueryEditor(
      this.query,
      this.queryEditor.props.totalMatches,
      suggestion,
    )
  }

  private initQueryEditor (firstQuery: string) {
    this.updateQueryEditor(firstQuery, 0)
  }

  private updateQueryEditor (newQuery: string, newTotalMatches: number, suggestion?: string) {
    let elem = React.createElement(QueryEditor, {
      query: newQuery,
      totalMatches: newTotalMatches,
      suggestion: suggestion || '',
      onChange: this.whenTheQueryChanges.bind(this),
      onAccept: this.whenTheSuggestionIsAccepted.bind(this),
      onReject: this.whenTheSuggestionIsRejected.bind(this),
      onAsk: this.whenUserRequestsFix.bind(this),
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
