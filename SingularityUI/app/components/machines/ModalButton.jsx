import React, { PropTypes } from 'react';
import FormModal from '../common/FormModal';
import {OverlayTrigger, Tooltip} from 'react-bootstrap/lib';

class ModalButton extends React.Component {

  button() {
    return <a onClick={() => this.refs.modal.show()} >{this.props.buttonChildren}</a>;
  }

  buttonWithMaybeTooltip() {
    if (this.props.tooltipText) {
      return (
        <OverlayTrigger
          placement="top"
          overlay={<Tooltip id="overlay">{this.props.tooltipText}</Tooltip>}>
          {this.button()}
        </OverlayTrigger>
      );
    }
    return this.button();
  }

  render() {
    return (
      <span>
        {this.buttonWithMaybeTooltip()}
        <FormModal
          ref="modal"
          action={this.props.action}
          onConfirm={(data) => this.props.onConfirm(data)}
          buttonStyle="danger"
          formElements={[
            {
              name: 'message',
              type: FormModal.INPUT_TYPES.STRING,
              label: 'Message (optional)'
            }
          ]}>
          {this.props.children}
        </FormModal>
      </span>
    );
  }
}

ModalButton.propTypes = {
  buttonChildren: PropTypes.node.isRequired,
  action: PropTypes.string.isRequired,
  onConfirm: PropTypes.func.isRequired,
  children: PropTypes.node.isRequired,
  tooltipText: PropTypes.string
};

export default ModalButton;
