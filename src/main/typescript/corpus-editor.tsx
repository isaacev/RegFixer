import { Component } from 'react'
import Editor from './editor'
import Match from './match'

interface CorpusEditorProps {
  query: string
  corpus: string
  onCorpusChange: (value: string) => void
  onMatchChange: (newMatches: Match[]) => void
  onMatchEdit: () => void
}

export default class CorpusEditor extends Component<CorpusEditorProps, {}> {
  handleCorpusChange (newCorpus: string) {
    this.props.onCorpusChange(newCorpus)
    this.props.onMatchChange([])
  }

  render () {
    return (
      <div className="corpus">
        <Editor
          value={this.props.corpus}
          onChange={this.handleCorpusChange.bind(this)} />
      </div>
    )
  }
}
