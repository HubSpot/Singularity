import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import Utils from '../../utils';

import { FetchRequestHistory } from '../../actions/api/history';

import Section from '../common/Section';

import UITable from '../common/table/UITable';
import Column from '../common/table/Column';
import JSONButton from '../common/JSONButton';

const RequestHistoryTable = ({requestId, requestEventsAPI, fetchRequestHistory}) => {
  const requestEvents = requestEventsAPI ? requestEventsAPI.data : [];
  const isFetching = requestEventsAPI ? requestEventsAPI.isFetching : false;
  const emptyTableMessage = (Utils.api.isFirstLoad(requestEventsAPI)
    ? 'Loading...'
    : 'No request history'
  );
  return (
    <Section id="request-history" title="Request history">
      <UITable
        emptyTableMessage={emptyTableMessage}
        data={requestEvents}
        keyGetter={(requestEvent) => requestEvent.createdAt}
        rowChunkSize={5}
        paginated={true}
        fetchDataFromApi={(page, numberPerPage) => fetchRequestHistory(requestId, numberPerPage, page)}
        isFetching={isFetching}
      >
        <Column
          label="State"
          id="state"
          key="state"
          cellData={(requestEvent) => Utils.humanizeText(requestEvent.eventType)}
        />
        <Column
          label="User"
          id="user"
          key="user"
          cellData={(requestEvent) => (requestEvent.user || '').split('@')[0]}
        />
        <Column
          label="Timestamp"
          id="timestamp"
          key="timestamp"
          cellData={(requestEvent) => Utils.timestampFromNow(requestEvent.createdAt)}
        />
        <Column
          label="Message"
          id="message"
          key="message"
          cellData={(requestEvent) => requestEvent.message}
        />
        <Column
          id="actions-column"
          key="actions-column"
          className="actions-column"
          cellData={(requestEvent) => <JSONButton object={requestEvent} showOverlay={true}>{'{ }'}</JSONButton>}
        />
      </UITable>
    </Section>
  );
};

RequestHistoryTable.propTypes = {
  requestId: PropTypes.string.isRequired,
  requestEventsAPI: PropTypes.object.isRequired,
  fetchRequestHistory: PropTypes.func.isRequired
};

const mapDispatchToProps = (dispatch) => ({
  fetchRequestHistory: (requestId, count, page) => dispatch(FetchRequestHistory.trigger(requestId, count, page))
});

const mapStateToProps = (state, ownProps) => ({
  requestEventsAPI: Utils.maybe(
    state.api.requestHistory,
    [ownProps.requestId]
  )
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(RequestHistoryTable);
