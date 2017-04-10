//
// src/main/typescript/app.tsx
// RegEx Frontend
//
// Created on 4/7/17
//

import * as request from 'superagent'
import { Component } from 'react'
import { RegexEditor } from './regex-editor'
import { RegexEditorControls } from './regex-editor-controls'
import { RegexEditorStatus } from './regex-editor-status'
import { FixModal } from './fix-modal'
import { CorpusEditor } from './corpus-editor'
import { Button } from './button'

interface Props {
  regex: string
  corpus: string
}

interface State {
  regex: string
  corpus: string
  matches: { start: number, end: number }[]
  hasFix: boolean
  fixedRegex: string
  inError: boolean
  message: string
}

export class App extends Component<Props, State> {
  constructor (props) {
    super(props)

    this.state = {
      regex: this.props.regex,
      corpus: this.props.corpus,
      matches: [],
      hasFix: false,
      fixedRegex: '',
      inError: false,
      message: '',
    }
  }

  handleRegexChange (newRegex: string): void {
    this.setState({
      regex: newRegex,
    })
  }

  handleRequestFix () {
    let matches = this.state.matches.map((match) => ({
      left: match.start,
      right: match.end,
    }))

    request
      .post('/api/fix')
      .send({
        regex: this.state.regex,
        ranges: matches,
        corpus: this.state.corpus,
      })
      .end((err, res) => {
        if (err != null || res.status !== 200) {
          console.error(err)
        } else {
          this.setState({
            hasFix: true,
            fixedRegex: res.text,
          })
        }
      })
  }

  handleAcceptFix () {
    this.setState({
      regex: this.state.fixedRegex,
      hasFix: false,
      fixedRegex: '',
    })
  }

  handleRejectFix () {
    this.setState({
      hasFix: false,
      fixedRegex: '',
    })
  }

  handleMatchesChange (matches: { start: number, end: number }[]): void {
    this.setState({
      matches: matches,
      inError: false,
      message: `${matches.length} matches`,
    })
  }

  handleInfiniteRegex (): void {
    this.setState({
      inError: true,
      message: 'Infinite',
    })
  }

  handleBrokenRegex (): void {
    this.setState({
      inError: true,
      message: 'Error',
    })
  }

  render () {
    let handleRegexChange   = this.handleRegexChange.bind(this)
    let handleRequestFix    = this.handleRequestFix.bind(this)
    let handleAcceptFix     = this.handleAcceptFix.bind(this)
    let handleRejectFix     = this.handleRejectFix.bind(this)
    let handleMatchesChange = this.handleMatchesChange.bind(this)
    let handleInfiniteRegex = this.handleInfiniteRegex.bind(this)
    let handleBrokenRegex   = this.handleBrokenRegex.bind(this)

    return (
      <div>
        <RegexEditor regex={this.state.regex} onRegexChange={handleRegexChange}>
          <RegexEditorControls>
            <Button glyph="?" color="blue" onClick={handleRequestFix} />
            <RegexEditorStatus inError={this.state.inError}>
              {this.state.message}
            </RegexEditorStatus>
          </RegexEditorControls>
          {(this.state.hasFix) && (
            <FixModal regex={this.state.fixedRegex}>
              <Button glyph="\u2713" color="green" onClick={handleAcceptFix} />
              <Button glyph="\u2717" color="red" onClick={handleRejectFix} />
            </FixModal>
          )}
        </RegexEditor>
        <CorpusEditor
          regex={this.state.regex}
          corpus={this.state.corpus}
          onMatchesChange={handleMatchesChange}
          onInfiniteRegex={handleInfiniteRegex}
          onBrokenRegex={handleBrokenRegex} />
      </div>
    )
  }
}