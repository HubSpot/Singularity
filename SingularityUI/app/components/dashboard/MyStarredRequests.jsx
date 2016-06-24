import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import { Row, Col } from 'react-bootstrap';

import UITable from '../common/table/UITable';
import { Starred, RequestId, Type, LastDeploy, DeployUser, State } from '../requests/Columns';

import * as StarredActions from '../../actions/ui/starred';
import * as RequestsSelectors from '../../selectors/requests';

const MyStarredRequests = ({requests, changeStar}) => {
  let starredRequestsSection = (
    <div className="empty-table-message"><p>No starred requests</p></div>
  );
  const starredRequests = requests.filter((r) => r.starred);

  if (starredRequests.length > 0) {
    starredRequestsSection = (
      <UITable
        data={starredRequests}
        keyGetter={(r) => r.request.id}
        asyncSort={true}
        paginated={true}
        rowChunkSize={10}
      >
        {Starred({changeStar})}
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
  requests: PropTypes.arrayOf(PropTypes.object).isRequired,
  changeStar: PropTypes.func.isRequired
};

const mapStateToProps = (state) => {
  return {
    requests: RequestsSelectors.combineStarredWithRequests(state)
  };
};

const mapDispatchToProps = (dispatch) => {
  return {
    changeStar: (requestId) => {
      dispatch(StarredActions.changeRequestStarAndSave(requestId));
    }
  };
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(MyStarredRequests);
