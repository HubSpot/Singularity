import React, { PropTypes } from 'react';
import Utils from '../../utils';
import { refresh } from '../../actions/ui/webhooks';
import { connect } from 'react-redux';
import Column from '../common/table/Column';
import UITable from '../common/table/UITable';
import rootComponent from '../../rootComponent';
import DeleteWebhookButton from '../common/modalButtons/DeleteWebhookButton';
import NewWebhookButton from '../common/modalButtons/NewWebhookButton';

const Webhooks = ({webhooks, user}) => (
  <div>
    <div className="row">
      <div className="col-md-10 col-xs-6">
        <span className="h1">Webhooks</span>
      </div>
      <div className="col-md-2 col-xs-6 button-container">
        <NewWebhookButton user={user}>
          <button
            className="btn btn-success pull-right"
            alt="Create a new webhook"
            title="newWebhook">
            New Webhook
          </button>
        </NewWebhookButton>
      </div>
    </div>
    <UITable
      emptyTableMessage="No Webhooks"
      data={webhooks}
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
  </div>
);

Webhooks.propTypes = {
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
};

function mapStateToProps(state) {
  const user = Utils.maybe(state, ['api', 'user', 'data', 'user', 'name']);
  return {
    user,
    webhooks: state.api.webhooks.data
  };
}

export default connect(mapStateToProps)(rootComponent(Webhooks, refresh));
