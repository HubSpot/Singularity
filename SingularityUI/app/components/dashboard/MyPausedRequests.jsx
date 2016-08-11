import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import { Row, Col } from 'react-bootstrap';

import UITable from '../common/table/UITable';
import { RequestId, Type, LastDeploy, Actions } from '../requests/Columns';

import * as RequestsSelectors from '../../selectors/requests';

const MyPausedRequests = ({userRequests}) => {
  let pausedRequestsSection = (
    <div className="empty-table-message"><p>No paused requests</p></div>
  );

  const pausedRequests = userRequests.filter((r) => r.state === 'PAUSED');

  if (pausedRequests.length > 0) {
    pausedRequestsSection = (
      <UITable
        data={pausedRequests}
        keyGetter={(r) => r.request.id}
        asyncSort={true}
        paginated={true}
        rowChunkSize={10}
      >
        {RequestId}
        {Type}
        {LastDeploy}
        {Actions}
      </UITable>
    );
  }

  return (
    <Row>
      <Col md={12} className="table-staged">
        <div className="page-header">
          <h2>My paused requests</h2>
        </div>
        {pausedRequestsSection}
      </Col>
    </Row>
  );
};

MyPausedRequests.propTypes = {
  userRequests: PropTypes.arrayOf(PropTypes.object).isRequired
};

const mapStateToProps = (state) => {
  return {
    userRequests: RequestsSelectors.getUserRequests(state)
  };
};

export default connect(
  mapStateToProps
)(MyPausedRequests);
