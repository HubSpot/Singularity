import React, { PropTypes, Component } from 'react';
import Utils from '../../utils';
import FormModal from '../common/modal/FormModal';
import { FetchWebhooks, NewWebhook } from '../../actions/api/webhooks';
import { connect } from 'react-redux';
import Column from '../common/table/Column';
import UITable from '../common/table/UITable';
import rootComponent from '../../rootComponent';
import DeleteWebhookButton from './DeleteWebhookButton';

const webhookTypes = ['REQUEST', 'DEPLOY', 'TASK'];

class Webhooks extends Component {

  static propTypes = {
    webhooks: PropTypes.arrayOf(PropTypes.shape({
      webhook: PropTypes.shape({
        uri: PropTypes.string.isRequired,
        id: PropTypes.string.isRequired,
        type: PropTypes.string.isRequired,
        timestamp: PropTypes.number.isRequired,
        user: PropTypes.string,
        queueSize: PropTypes.number
      }).isRequired,
      queueSize: PropTypes.number
    })).isRequired,
    user: PropTypes.string,
    fetchWebhooks: PropTypes.func.isRequired,
    newWebhook: PropTypes.func.isRequired
  };

  constructor(props) {
    super(props);
    this.state = {};
    _.bindAll(this, 'newWebhook', 'promptNewWebhook');
  }

  checkWebhookUri(uri) {
    try {
      return !new URL(uri);
    } catch (err) {
      return 'Invalid URL';
    }
  }

  newWebhook(uri, type) {
    this.props.newWebhook(uri, type, this.props.user).then(this.props.fetchWebhooks());
  }

  promptNewWebhook() {
    this.refs.newWebhookModal.show();
  }

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
            options: webhookTypes.map((type) => ({
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
  }

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
        <UITable
          emptyTableMessage="No Webhooks"
          data={this.props.webhooks}
          keyGetter={(webhook) => webhook.webhook.timestamp}
          rowChunkSize={20}
          paginated={true}
          defaultSortBy="queue-size"
          defaultSortDirection={UITable.SortDirection.ASC}
        >
          <Column
            label="URL"
            id="url"
            key="url"
            sortable={true}
            sortData={(cellData, webhook) => webhook.webhook.uri}
            cellData={(webhook) => webhook.webhook.uri}
          />
          <Column
            label="Type"
            id="type"
            key="type"
            sortable={true}
            sortData={(cellData, webhook) => webhook.webhook.type}
            cellData={(webhook) => Utils.humanizeText(webhook.webhook.type)}
          />
          <Column
            label="Timestamp"
            id="timestamp"
            key="timestamp"
            sortable={true}
            sortData={(cellData, webhook) => webhook.webhook.timestamp}
            cellData={(webhook) => Utils.absoluteTimestamp(webhook.webhook.timestamp)}
          />
          <Column
            label="User"
            id="user"
            key="user"
            sortable={true}
            sortData={(cellData, webhook) => webhook.webhook.user || 'N/A'}
            cellData={(webhook) => webhook.webhook.user || 'N/A'}
          />
          <Column
            label="Queue Size"
            id="queue-size"
            key="queue-size"
            sortable={true}
            sortData={(cellData, webhook) => webhook.queueSize}
            cellData={(webhook) => webhook.queueSize && <b>{webhook.queueSize}</b> || 0}
          />
          <Column
            id="actions-column"
            key="actions-column"
            className="actions-column"
            cellData={(webhook) => <DeleteWebhookButton webhook={webhook.webhook} />}
          />
        </UITable>
        {this.renderNewWebhookModal()}
      </div>
    );
  }
}

function mapStateToProps(state) {
  const user = Utils.maybe(state, ['api', 'user', 'data', 'user', 'name']);
  return {
    user,
    webhooks: state.api.webhooks.data
  };
}

function mapDispatchToProps(dispatch) {
  return {
    newWebhook: (uri, type, user) => dispatch(NewWebhook.trigger(uri, type, user)),
    fetchWebhooks: () => dispatch(FetchWebhooks.trigger())
  };
}

function refresh(props) {
  return props.fetchWebhooks();
}

export default connect(mapStateToProps, mapDispatchToProps)(rootComponent(Webhooks, 'Webhooks', refresh));
