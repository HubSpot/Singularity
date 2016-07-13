import React, {PropTypes} from 'react';
import Utils from '../../utils';
import FormModal from '../common/modal/FormModal';
import Glyphicon from '../common/atomicDisplayItems/Glyphicon';
import { FetchWebhooks, DeleteWebhook, NewWebhook } from '../../actions/api/webhooks';
import { connect } from 'react-redux';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';
import SimpleTable from '../common/SimpleTable';

const Webhooks = React.createClass({

  propTypes: {
    api: PropTypes.shape({
      webhooks: PropTypes.shape({
        data: PropTypes.arrayOf(PropTypes.shape({
          //
        }))
      }).isRequired
    }),
    webhooks: PropTypes.arrayOf(PropTypes.shape({
      uri: PropTypes.string.isRequired,
      id: PropTypes.string.isRequired,
      type: PropTypes.string.isRequired,
      timestamp: PropTypes.number.isRequired,
      user: PropTypes.string,
      queueSize: PropTypes.number
    })).isRequired,
    fetchWebhooks: PropTypes.func.isRequired,
    newWebhook: PropTypes.func.isRequired,
    deleteWebhook: PropTypes.func.isRequired
  },

  getInitialState() { return {}; },

  webhookTypes: ['REQUEST', 'DEPLOY', 'TASK'],

  checkWebhookUri(uri) {
    try {
      return !new URL(uri);
    } catch (err) {
      return 'Invalid URL';
    }
  },

  deleteWebhook(webhook) {
    this.props.deleteWebhook(webhook.id).then(this.props.fetchWebhooks());
  },

  promptDeleteWebhook(webhookToDelete) {
    this.setState({webhookToDelete});
    this.refs.deleteModal.show();
  },

  newWebhook(uri, type) {
    let user;
    if (app.user && app.user.attributes.authenticated) {
      user = app.user.attributes.user.id;
    }
    this.props.newWebhook(uri, type, user).then(this.props.fetchWebhooks());
  },

  promptNewWebhook() {
    this.refs.newWebhookModal.show();
  },

  renderDeleteWebhookLink(webhook) {
    const toolTip = <ToolTip id={`delete-${ webhook.id }`}>Delete This Webhook</ToolTip>;
    return (
      <OverlayTrigger placement="top" overlay={toolTip}>
        <a onClick={() => this.promptDeleteWebhook(webhook)}>
          <Glyphicon iconClass="trash" />
        </a>
      </OverlayTrigger>
    );
  },

  renderDeleteWebhookModal() {
    const webhookToDelete = this.state.webhookToDelete;
    return (
      <FormModal
        ref="deleteModal"
        name="Delete Webhook"
        action="Delete Webhook"
        onConfirm={() => this.deleteWebhook(webhookToDelete)}
        buttonStyle="danger"
        formElements={[]}>
        <div>
          {webhookToDelete &&
            <pre>
              ({webhookToDelete.type}) {webhookToDelete.uri}
            </pre>}
          <p>
            Are you sure you want to delete this webhook?
          </p>
        </div>
      </FormModal>
    );
  },

  renderNewWebhookModal() {
    return (
      <FormModal
        ref="newWebhookModal"
        name="New Webhook"
        action="Create Webhook"
        buttonStyle="success"
        onConfirm={(data) => this.newWebhook(data.uri, data.type)}
        formElements={[
          {
            type: FormModal.INPUT_TYPES.SELECT,
            name: 'type',
            label: 'Type',
            isRequired: true,
            options: this.webhookTypes.map((type) => ({
              label: Utils.humanizeText(type),
              value: type
            }))
          },
          {
            type: FormModal.INPUT_TYPES.STRING,
            name: 'uri',
            label: 'URI',
            isRequired: true,
            validateField: (value) => this.checkWebhookUri(value)
          }
        ]}
      />
    );
  },

  render() {
    return (
      <div>
        <div className="row">
          <div className="col-md-10 col-xs-6">
            <span className="h1">Webhooks</span>
          </div>
          <div className="col-md-2 col-xs-6 button-container">
            <button
              className="btn btn-success pull-right"
              alt="Create a new webhook"
              title="newWebhook"
              onClick={this.promptNewWebhook}>
              New Webhook
            </button>
          </div>
        </div>
        <SimpleTable
          emptyMessage="No Webhooks"
          entries={this.props.webhooks}
          perPage={20}
          headers={['URL', 'Type', 'Timestamp', 'User', 'Queue Size', '']}
          renderTableRow={(webhook, index) => {
            return (
              <tr key={index}>
                <td>{webhook.uri}</td>
                <td>{Utils.humanizeText(webhook.type)}</td>
                <td>{Utils.absoluteTimestamp(webhook.timestamp)}</td>
                <td>{webhook.user || 'N/A'}</td>
                <td>{webhook.queueSize || 0}</td>
                <td>{this.renderDeleteWebhookLink(webhook)}</td>
              </tr>
            );
          }}
        />
        {this.renderNewWebhookModal()}
        {this.renderDeleteWebhookModal()}
      </div>
    );
  }
});

function mapStateToProps(state) {
  return {
    webhooks: state.api.webhooks.data
  };
}

function mapDispatchToProps(dispatch) {
  return {
    fetchWebhooks() { return dispatch(FetchWebhooks.trigger()); },
    newWebhook(uri, type, user) { return dispatch(NewWebhook.trigger(uri, type, user)); },
    deleteWebhook(id) { return dispatch(DeleteWebhook.trigger(id)); }
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(Webhooks);

