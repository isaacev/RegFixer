//
// src/app.ts
// RegEx Frontend
//
// Created on 2/20/17
//

import { QueryEditor, BubbleStatus } from './query-editor'
import { CorpusEditor } from './corpus-editor'
import { InfiniteMatchesError } from './errors'
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

    this.query.onChange = (isEmpty: boolean, regex: RegExp) => {
      this.query.hideBubble()
      this.corpus.clearRegions()

      if (isEmpty === false && regex !== null) {
        let totalMatches = -1

        try {
          totalMatches = this.corpus.findMatches(regex)
        } catch (err) {
          this.query.hideBubble()
          this.corpus.clearRegions()

          switch (true) {
            case (err instanceof InfiniteMatchesError):
              this.query.setBubble('infinite regex', BubbleStatus.red)
              break
            default:
              throw err
          }
        }

        if (totalMatches > -1) {
          this.query.setBubble(`${totalMatches} matches`)
        }
      }
    }
  }
}
