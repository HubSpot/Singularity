import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import Utils from '../../utils';

import { FetchRequestHistory } from '../../actions/api/history';

import ServerSideTable from '../common/ServerSideTable';
import JSONButton from '../common/JSONButton';

const RequestHistoryTable = ({requestId, requestEvent}) => (
  <div>
    <h2>Request history</h2>
    <ServerSideTable
      emptyMessage="No request history"
      entries={requestEvent}
      paginate={requestEvent.length >= 5}
      perPage={5}
      fetchAction={FetchRequestHistory}
      fetchParams={[requestId]}
      headers={['State', 'User', 'Created', 'Message', '']}
      renderTableRow={(data, index) => {
        const { eventType, user, createdAt, message } = data;

        return (
          <tr key={index}>
            <td>
              {Utils.humanizeText(eventType)}
            </td>
            <td>
              {(user || '').split('@')[0]}
            </td>
            <td>
              {Utils.timestampFromNow(createdAt)}
            </td>
            <td>
              {message}
            </td>
            <td className="actions-column">
              <JSONButton object={data}>{'{ }'}</JSONButton>
            </td>
          </tr>
        );
      }}
    />
  </div>
);

RequestHistoryTable.propTypes = {
  requestId: PropTypes.string.isRequired,
  requestEvent: PropTypes.arrayOf(PropTypes.object).isRequired
};

const mapStateToProps = (state, ownProps) => ({
  requestEvent: Utils.maybe(state.api.requestHistory, [ownProps.requestId, 'data'])
});

export default connect(
  mapStateToProps,
  null
)(RequestHistoryTable);
