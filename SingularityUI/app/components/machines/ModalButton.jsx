import React, { PropTypes } from 'react';
import FormModal from '../common/modal/FormModal';
import {OverlayTrigger, Tooltip} from 'react-bootstrap';

const ModalButton = (props) => {
  let modal;
  const button = <a onClick={() => modal.show()} >{props.buttonChildren}</a>;
  let buttonWithMaybeTooltip = button;
  if (props.tooltipText) {
    buttonWithMaybeTooltip = (
      <OverlayTrigger
        placement="top"
        overlay={<Tooltip id="overlay">{props.tooltipText}</Tooltip>}>
        {button}
      </OverlayTrigger>
    );
  }
  return (
    <span>
      {buttonWithMaybeTooltip}
      <FormModal
        ref={modalRef => { modal = modalRef; }}
        action={props.action}
        onConfirm={(data) => props.onConfirm(data)}
        buttonStyle="danger"
        formElements={props.formElements}>
        {props.children}
      </FormModal>
    </span>
  );
};

ModalButton.propTypes = {
  buttonChildren: PropTypes.node.isRequired,
  action: PropTypes.string.isRequired,
  onConfirm: PropTypes.func.isRequired,
  children: PropTypes.node.isRequired,
  tooltipText: PropTypes.string,
  formElements: PropTypes.arrayOf(PropTypes.shape({
    name: PropTypes.string.isRequired,
    type: PropTypes.string.isRequired,
    label: PropTypes.string
  }))
};

export default ModalButton;
