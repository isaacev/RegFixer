import { Component } from 'react'
import Editor from './editor'
import QueryControls from './query-controls'
import { BubbleStatus } from './status-bubble'
import Modal from './modal'

interface QueryEditorProps {
  query: string
  totalMatches: number
  onChange: (value: string) => void
}

export default class QueryEditor extends Component<QueryEditorProps, {}> {
  static defaultProps: Partial<QueryEditorProps> = {
    onChange: (() => {}),
  }

  render () {
    let showModal = false

    return (
      <div className="query">
        <Editor
          value={this.props.query}
          onChange={this.props.onChange} />
        <QueryControls
          status={BubbleStatus.Normal}
          totalMatches={this.props.totalMatches} />
        {showModal && (
          <Modal
            regex={this.props.query} />
        )}
      </div>
    )
  }
}
