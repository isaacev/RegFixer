import { PureComponent } from 'react'
import ActionButton, { ActionButtonColor } from './action-button'

interface ModalHeaderProps {
  regex: string
}

export default class ModalHeader extends PureComponent<ModalHeaderProps, {}> {
  render () {
    return (
      <div className="modal-header">
        <p className="regex">{this.props.regex}</p>
        <ActionButton name="accept" color={ActionButtonColor.Green} glyph="\u2713" />
        <ActionButton name="reject" color={ActionButtonColor.Red} glyph="\u2717" />
      </div>
    )
  }
}
