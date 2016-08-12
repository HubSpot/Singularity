import React, { Component, PropTypes } from 'react';
import { connect } from 'react-redux';

import { DeleteWebhook, FetchWebhooks } from '../../../actions/api/webhooks';

import FormModal from '../modal/FormModal';

class DeleteWebhookModal extends Component {
  static propTypes = {
    webhook: PropTypes.shape({
      id: PropTypes.string.isRequired,
      type: PropTypes.string.isRequired,
      uri: PropTypes.string.isRequired
    }).isRequired,
    deleteWebhook: PropTypes.func.isRequired
  };

  show() {
    this.refs.deleteModal.show();
  }

  render() {
    return (
      <FormModal
        ref="deleteModal"
        name="Delete Webhook"
        action="Delete Webhook"
        onConfirm={this.props.deleteWebhook}
        buttonStyle="danger"
        formElements={[]}>
        <div>
          <pre>
            ({this.props.webhook.type}) {this.props.webhook.uri}
          </pre>
          <p>
            Are you sure you want to delete this webhook?
          </p>
        </div>
      </FormModal>
    );
  }
}

const mapDispatchToProps = (dispatch, ownProps) => ({
  deleteWebhook: () => dispatch(DeleteWebhook.trigger(ownProps.webhook.id)).then(() => dispatch(FetchWebhooks.trigger())),
});

export default connect(
  null,
  mapDispatchToProps,
  null,
  { withRef: true }
)(DeleteWebhookModal);
