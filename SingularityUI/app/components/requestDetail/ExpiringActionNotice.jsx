import React, { PropTypes } from 'react';
import { Alert, Button } from 'react-bootstrap';

import Utils from '../../utils';

const ExpiringActionNotice = (props) => {
  let maybeCanRevert;
  if (props.canRevert) {
    maybeCanRevert = props.revertButton || (
      <Button bsStyle="primary" bsSize="xsmall" onClick={props.revertAction}>
        {props.revertText}
      </Button>
    );
  }

  let maybeMessage;
  if (props.message) {
    maybeMessage = (
      <p>
        <span>{props.user} said: </span>
        <em>{props.message}</em>
      </p>
    );
  }

  let persist = props.persistButton || (
    <Button bsStyle="default" bsSize="xsmall" onClick={props.persistAction}>
      {props.persistText}
    </Button>
  );

  return (
    <Alert bsStyle="info">
      <strong>Action Expiration: </strong>
      <span> This request has an active </span>
      <strong>{props.action}</strong>
      <span> request that expires </span>
      <span>{Utils.timestampFromNow(props.endMillis)}.</span>
      <span> {persist} </span>
      {maybeCanRevert}
      {maybeMessage}
    </Alert>
  );
};

ExpiringActionNotice.propTypes = {
  action: PropTypes.string,
  user: PropTypes.string,
  endMillis: PropTypes.number,
  canRevert: PropTypes.bool,
  persistButton: PropTypes.node, // only use a button OR the text and action
  persistText: PropTypes.string,
  persistAction: PropTypes.func,
  revertButton: PropTypes.node, // only use a button OR the text and action
  revertText: PropTypes.string,
  revertAction: PropTypes.func,
  message: PropTypes.string
};

export default ExpiringActionNotice;
