import { PureComponent } from 'react'
import ModalHeader from './modal-header'
import ModalExplanation from './modal-explanation'

interface ModalProps {
  regex: string
}

export default class Modal extends PureComponent<ModalProps, {}> {
  render () {
    return (
      <div className="modal">
        <div className="modal-triangle"></div>
        <ModalHeader regex={this.props.regex} />
        <ModalExplanation />
      </div>
    )
  }
}
