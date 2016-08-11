import React, { PropTypes } from 'react';
import FormModal from '../common/modal/FormModal';
import {OverlayTrigger, Tooltip} from 'react-bootstrap';

const ModalButton = (props) => {
  let buttonWithMaybeTooltip;
  let modal;
  if (props.tooltipText) {
    buttonWithMaybeTooltip = (
      <OverlayTrigger
        placement="top"
        overlay={<Tooltip id="overlay">{props.tooltipText}</Tooltip>}>
        <a onClick={() => modal.show()} >{props.buttonChildren}</a>
      </OverlayTrigger>
    );
  } else {
    buttonWithMaybeTooltip = <a onClick={() => modal.show()} >{props.buttonChildren}</a>;
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
