import ReactDOM from 'react-dom'
import App from './app'

ReactDOM.render(<App query="\\w+" corpus="foo bar baz" />, document.querySelector('.app'))
