import React, {PropTypes} from 'react';
import Utils from '../../utils';
import OldTable from '../common/OldTable';
import FormModal from '../common/modal/FormModal';
import PlainText from '../common/atomicDisplayItems/PlainText';
import TimeStamp from '../common/atomicDisplayItems/TimeStamp';
import Link from '../common/atomicDisplayItems/Link';
import Glyphicon from '../common/atomicDisplayItems/Glyphicon';
import { FetchWebhooks } from '../../actions/api/webhooks';
import { connect } from 'react-redux';

const Webhooks = React.createClass({

  propTypes: {
    api: PropTypes.shape({
      webhooks: PropTypes.shape({
        data: PropTypes.arrayOf(PropTypes.shape({
          //
        }))
      }).isRequired
    }),
    collections: PropTypes.shape({
      webhooks: PropTypes.array.isRequired
    }).isRequired,
    fetchWebhooks: PropTypes.func.isRequired
  },

  getInitialState() { return {}; },

  defaultRowsPerPage: 10,

  rowsPerPageChoices: [10, 20],

  webhookTypes: ['REQUEST', 'DEPLOY', 'TASK'],

  sortBy(field, sortDirectionAscending) {
    this.props.api.webhooks.data.sortBy(field, sortDirectionAscending);
    return this.forceUpdate();
  },

  webhookColumns() {
    const { sortBy } = this; // JS is annoying
    return [{
      data: 'URL',
      sortable: true,
      doSort: sortDirectionAscending => sortBy('uri', sortDirectionAscending)
    }, {
      data: 'Type',
      sortable: true,
      doSort: sortDirectionAscending => sortBy('type', sortDirectionAscending)
    }, {
      data: 'Timestamp',
      className: 'hidden-xs',
      sortable: true,
      doSort: sortDirectionAscending => sortBy('timestamp', sortDirectionAscending)
    }, {
      data: 'User',
      className: 'hidden-xs',
      sortable: true,
      doSort: sortDirectionAscending => sortBy('user', sortDirectionAscending)
    }, {
      data: 'Queue Size',
      sortable: true,
      doSort: sortDirectionAscending => sortBy('queueSize', sortDirectionAscending)
    }, {
      className: 'hidden-xs'
    }];
  },

  checkWebhookUri(uri) {
    try {
      return !new URL(uri);
    } catch (err) {
      return 'Invalid URL';
    }
  },

  deleteWebhook(webhook) {
    return $.ajax({
      url: `${ config.apiRoot }/webhooks/?webhookId=${ webhook.id }`,
      type: 'DELETE',
      success: () => this.props.fetchWebhooks()
    });
  },

  promptDeleteWebhook(webhookToDelete) {
    this.setState({webhookToDelete});
    this.refs.deleteModal.show();
  },

  newWebhook(uri, type) {
    const data = {
      uri,
      type
    };
    if (app.user && app.user.attributes.authenticated) {
      data.user = app.user.attributes.user.id;
    }
    return $.ajax({
      url: `${ config.apiRoot }/webhooks`,
      type: 'POST',
      contentType: 'application/json',
      data: JSON.stringify(data),
      success: () => this.props.fetchWebhooks()
    });
  },

  promptNewWebhook() {
    this.refs.newWebhookModal.show();
  },

  getWebhookTableData() {
    const data = [];
    this.props.collections.webhooks.map(webhook => data.push({
      dataId: webhook.id,
      dataCollection: 'webhooks',
      data: [{
        component: PlainText,
        prop: {
          text: webhook.uri
        }
      }, {
        component: PlainText,
        prop: {
          text: Utils.humanizeText(webhook.type)
        }
      }, {
        component: TimeStamp,
        className: 'hidden-xs',
        prop: {
          timestamp: webhook.timestamp,
          display: 'absoluteTimestamp'
        }
      }, {
        component: PlainText,
        className: 'hidden-xs',
        prop: {
          text: webhook.user || 'N/A'
        }
      }, {
        component: PlainText,
        prop: {
          text: <b>{webhook.queueSize}</b>
        }
      }, {
        component: Link,
        className: 'hidden-xs actions-column',
        prop: {
          text: <Glyphicon iconClass="trash" />,
          onClickFn: () => this.promptDeleteWebhook(webhook),
          title: 'Delete',
          altText: 'Delete this webhook',
          overlayTrigger: true,
          overlayTriggerPlacement: 'top',
          overlayToolTipContent: 'Delete This Webhook',
          overlayId: `deleteWebhook${ webhook.id }`
        }
      }]
    }));
    return data;
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
          <div className="col-md-10">
            <span className="h1">Webhooks</span>
          </div>
          <div className="col-md-2 button-container">
            <button
              className="btn btn-success"
              alt="Create a new webhook"
              title="newWebhook"
              onClick={this.promptNewWebhook}>
              New Webhook
            </button>
          </div>
        </div>
        <OldTable
          defaultRowsPerPage={this.defaultRowsPerPage}
          rowsPerPageChoices={this.rowsPerPageChoices}
          tableClassOpts="table-striped"
          columnHeads={this.webhookColumns()}
          tableRows={this.getWebhookTableData()}
          emptyTableMessage="No Webhooks"
          dataCollection="webhooks"
        />
        {this.renderNewWebhookModal()}
        {this.renderDeleteWebhookModal()}
      </div>
    );
  }
});

function mapStateToProps(state) {
  return {
    collections: {
      webhooks: state.api.webhooks.data
    }
  };
}

function mapDispatchToProps(dispatch) {
  return {
    fetchWebhooks() {
      dispatch(FetchWebhooks.trigger());
    }
  };
}

export default connect(mapStateToProps, mapDispatchToProps)(Webhooks);

