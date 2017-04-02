//
// src/typescript/app.ts
// RegEx Frontend
//
// Created on 2/20/17
//

import localforage from 'localforage'
import { QueryEditor, BubbleStatus } from './query-editor'
import { CorpusEditor } from './corpus-editor'
import * as util from './util'

export class App {
  wrapper: HTMLElement
  query: QueryEditor
  corpus: CorpusEditor

  constructor (wrapper: HTMLElement, palette: string[] = []) {
    this.wrapper = wrapper

    let queryElem = util.createElement('div', this.wrapper)
    util.addClass(queryElem, 'query')

    let corpusElem = util.createElement('div', this.wrapper)
    util.addClass(corpusElem, 'corpus')

    this.query = new QueryEditor(queryElem)
    this.corpus = new CorpusEditor(corpusElem, palette)

    // Using the debounce function, the local storage cache will be updated
    // with current query & corpus values after 1 second of no edits to the
    // query editor.
    let queryChangeDebounce = util.debounce(this.updateCache.bind(this), 1000)

    this.query.onEmpty = () => {
      this.query.hideBubble()
      this.corpus.clearRegex()
      this.corpus.clearRegions()
      queryChangeDebounce()
    }

    this.query.onChange = (regex) => {
      this.corpus.setRegex(regex)
      queryChangeDebounce()
    }

    this.corpus.onInfiniteMatches = () => {
      this.query.setBubble('infinite')
    }

    this.corpus.onMatches = (totalMatches: number) => {
      this.query.setBubble(`${totalMatches} matches`)
    }

    // When the page is loaded, check if there are any locally stored values
    // for the query & corpus editors.
    localforage.getItem<string>('regex-corpus', (err, val) => {
      if (err === null && val !== null) {
        this.corpus.setValue(val)
      } else {
        console.log('missing regex-corpus')
        this.corpus.setValue('foo bar\nbaz')
      }

      localforage.getItem<string>('regex-query', (err, val) => {
        if (err === null && val !== null) {
          this.query.setValue(val)
        } else {
          console.log('missing regex-query')
          this.query.setValue('[a-z]+')
        }
      })
    })
  }

  updateCache () {
    console.log('update cache')
    localforage.setItem<string>('regex-corpus', this.corpus.getValue())
    localforage.setItem<string>('regex-query', this.query.getValue())
  }

  clearCache () {
    console.log('clear cache')
    localforage.removeItem('regex-corpus')
    localforage.removeItem('regex-query')
  }
}
