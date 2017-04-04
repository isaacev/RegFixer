import { PureComponent } from 'react'

interface ModalExplanationProps {
  // ...
}

export default class ModalExplanation extends PureComponent<ModalExplanationProps, {}> {
  render () {
    return (
      <div className="modal-explanation">
        <h2>Explanation</h2>
        <p>Morbi leo risus, porta ac consectetur ac, vestibulum at eros.</p>
      </div>
    )
  }
}
