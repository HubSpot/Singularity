import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import { FetchWebhooks, NewWebhook } from '../../../actions/api/webhooks';

import FormModal from '../modal/FormModal';

const WEBHOOK_TYPES = ['REQUEST', 'DEPLOY', 'TASK'];

import Utils from '../../../utils';

const checkWebhookUri = (uri) => {
  try {
    return !new URL(uri);
  } catch (err) {
    return 'Invalid URL';
  }
};

class DeleteWebhookModal extends Component {
  static propTypes = {
    user: PropTypes.string,
    newWebhook: PropTypes.func.isRequired
  };

  show() {
    this.refs.newWebhookModal.show();
  }

  render() {
    return (
      <FormModal
        ref="newWebhookModal"
        name="New Webhook"
        action="Create Webhook"
        buttonStyle="success"
        onConfirm={(data) => this.props.newWebhook(data.uri, data.type)}
        formElements={[
          {
            type: FormModal.INPUT_TYPES.SELECT,
            name: 'type',
            label: 'Type',
            isRequired: true,
            options: WEBHOOK_TYPES.map((type) => ({
              label: Utils.humanizeText(type),
              value: type
            }))
          },
          {
            type: FormModal.INPUT_TYPES.STRING,
            name: 'uri',
            label: 'URI',
            isRequired: true,
            validateField: (value) => checkWebhookUri(value)
          }
        ]}
      />
    );
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  newWebhook: (uri, type) => dispatch(NewWebhook.trigger(uri, type, ownProps.user)).then(() => {dispatch(FetchWebhooks.trigger());}),
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(DeleteWebhookModal);
