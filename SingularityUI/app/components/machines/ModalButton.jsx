import React, { PropTypes } from 'react';
import FormModal from '../common/modal/FormModal';
import {OverlayTrigger, Tooltip} from 'react-bootstrap';

class ModalButton extends React.Component {

  buttonWithMaybeTooltip() {
    if (this.props.tooltipText) {
      return (
        <OverlayTrigger
          placement="top"
          overlay={<Tooltip id="overlay">{this.props.tooltipText}</Tooltip>}>
          <a onClick={() => this.refs.modal.show()} >{this.props.buttonChildren}</a>
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
          name={this.props.name}
          ref="modal"
          action={this.props.action}
          onConfirm={(data) => this.props.onConfirm(data)}
          buttonStyle="danger"
          formElements={this.props.formElements}>
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
  tooltipText: PropTypes.string,
  name: PropTypes.string,
  formElements: PropTypes.arrayOf(PropTypes.shape({
    name: PropTypes.string.isRequired,
    type: PropTypes.string.isRequired,
    label: PropTypes.string
  }))
};

export default ModalButton;
