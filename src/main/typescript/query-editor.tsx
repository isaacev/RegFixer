//
// src/main/typescript/query-editor.tsx
// RegEx Frontend
//
// Created on 4/7/17
//

import { Component } from 'react'
import { Editor } from './editor'
import { QueryControls } from './query-controls'
import { QueryModal } from './query-modal'

interface QueryEditorProps {
  query: string
  totalMatches: number
  suggestion?: string
  onChange: (value: string) => void
  onAsk: (oldQuery: string) => void
  onAccept: (newQuery: string) => void
  onReject: () => void
}

export class QueryEditor extends Component<QueryEditorProps, {}> {
  handleAsk () {
    this.props.onAsk(this.props.query)
  }

  handleAccept () {
    this.props.onAccept(this.props.suggestion)
  }

  handleReject () {
    this.props.onReject()
  }

  render () {
    return (
      <div className="query-editor">
        <Editor value={this.props.query} onChange={this.props.onChange} />
        <QueryControls onAsk={this.handleAsk.bind(this)} totalMatches={this.props.totalMatches} />
        {(this.props.suggestion !== '') && (
          <QueryModal
            onAccept={this.handleAccept.bind(this)}
            onReject={this.handleReject.bind(this)}
            replacement={this.props.suggestion} />
        )}
      </div>
    )
  }
}
