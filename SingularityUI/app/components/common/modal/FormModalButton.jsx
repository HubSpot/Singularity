import React, { PropTypes } from 'react';
import FormModal from './FormModal';
import {OverlayTrigger, Tooltip} from 'react-bootstrap';

const FormModalButton = (props) => {
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
        name={props.name}
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

FormModalButton.propTypes = {
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

export default FormModalButton;
