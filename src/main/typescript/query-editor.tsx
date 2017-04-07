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
  onChange: (value: string) => void
}

export class QueryEditor extends Component<QueryEditorProps, {}> {
  render () {
    return (
      <div className="query-editor">
        <Editor value={this.props.query} onChange={this.props.onChange} />
        <QueryControls totalMatches={this.props.totalMatches} />
        {false && (
          <QueryModal replacement={this.props.query} />
        )}
      </div>
    )
  }
}
