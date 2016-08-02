import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import { Row, Col } from 'react-bootstrap';

import UITable from '../common/table/UITable';
import { Starred, RequestId, Type, LastDeploy, DeployUser, State } from '../requests/Columns';

import * as RequestsSelectors from '../../selectors/requests';

const MyStarredRequests = ({starredRequests}) => {
  let starredRequestsSection = (
    <div className="empty-table-message"><p>No starred requests</p></div>
  );

  if (starredRequests.length > 0) {
    starredRequestsSection = (
      <UITable
        data={starredRequests}
        keyGetter={(r) => r.request.id}
        asyncSort={true}
        paginated={true}
        rowChunkSize={10}
      >
        {Starred}
        {RequestId}
        {Type}
        {LastDeploy}
        {DeployUser}
        {State}
      </UITable>
    );
  }

  return (
    <Row>
      <Col md={12} className="table-staged">
        <div className="page-header">
          <h2>Starred requests</h2>
        </div>
        {starredRequestsSection}
      </Col>
    </Row>
  );
};

MyStarredRequests.propTypes = {
  starredRequests: PropTypes.arrayOf(PropTypes.object).isRequired,
};

const mapStateToProps = (state) => {
  return {
    starredRequests: RequestsSelectors.getStarredRequests(state)
  };
};

export default connect(
  mapStateToProps
)(MyStarredRequests);
