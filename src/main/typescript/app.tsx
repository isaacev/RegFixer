import { Component } from 'react'
import QueryEditor from './query-editor'
import CorpusEditor from './corpus-editor'
import Match from './match'

interface AppProps {
  query: string
  corpus: string
}

interface AppState {
  isClean: boolean
  query: string
  corpus: string
  matches: Match[]
}

export default class App extends Component<AppProps, AppState> {
  constructor (props) {
    super(props)

    this.state = {
      isClean: true,
      query: props.query,
      corpus: props.corpus,
      matches: [],
    }
  }

  handleQueryChange (newQuery: string) {
    this.setState({
      isClean: true,
      query: newQuery,
    })
  }

  handleCorpusChange (newCorpus: string) {
    this.setState({
      isClean: true,
      corpus: newCorpus,
    })
  }

  handleMatchChange (newMatches: Match[]) {
    this.setState({
      matches: newMatches,
    })
  }

  handleMatchEdit () {
    this.setState({
      isClean: false,
    })
  }

  render () {
    return (
      <div className="editors">
        <QueryEditor
          query={this.state.query}
          totalMatches={this.state.matches.length}
          onChange={this.handleQueryChange.bind(this)} />
        <CorpusEditor
          query={this.state.query}
          corpus={this.state.corpus}
          onCorpusChange={this.handleCorpusChange.bind(this)}
          onMatchChange={this.handleMatchChange.bind(this)}
          onMatchEdit={this.handleMatchEdit.bind(this)} />
      </div>
    )
  }
}
