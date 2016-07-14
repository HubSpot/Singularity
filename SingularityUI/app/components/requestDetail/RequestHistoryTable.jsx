import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import Utils from '../../utils';

import { FetchRequestHistory } from '../../actions/api/history';

import Section from '../common/Section';

import ServerSideTable from '../common/ServerSideTable';
import JSONButton from '../common/JSONButton';

const RequestHistoryTable = ({requestId, requestEventsAPI}) => {
  const requestEvents = requestEventsAPI ? requestEventsAPI.data : [];
  const emptyTableMessage = (Utils.api.isFirstLoad(requestEventsAPI)
    ? 'Loading...'
    : 'No request history'
  );
  return (
    <Section id="deploy-history" title="Request history">
      <ServerSideTable
        emptyMessage={emptyTableMessage}
        entries={requestEvents}
        paginate={requestEvents.length >= 5}
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
    </Section>
  );
};

RequestHistoryTable.propTypes = {
  requestId: PropTypes.string.isRequired,
  requestEventsAPI: PropTypes.object.isRequired
};

const mapStateToProps = (state, ownProps) => ({
  requestEventsAPI: Utils.maybe(
    state.api.requestHistory,
    [ownProps.requestId]
  )
});

export default connect(
  mapStateToProps
)(RequestHistoryTable);
