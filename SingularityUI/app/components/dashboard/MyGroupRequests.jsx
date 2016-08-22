import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

import { Row, Col, ButtonGroup, DropdownButton, MenuItem } from 'react-bootstrap';

import UITable from '../common/table/UITable';
import { RequestId, Type, LastDeploy, Actions } from '../requests/Columns';
import Utils from '../../utils';
import { SetDashboardGroup } from '../../actions/ui/dashboard';
import * as RequestsSelectors from '../../selectors/requests';

const MyGroupRequests = ({groupRequests, groups, currentGroup, setCurrentGroup}) => {
  if (groups.length == 0) {
    return null;
  }

  let requestsSection = (
    <div className="empty-table-message"><p>No requests</p></div>
  );

  if (groupRequests.length > 0) {
    requestsSection = (
      <UITable
        data={groupRequests}
        keyGetter={(requestParent) => requestParent.request.id}
        asyncSort={true}
        renderAllRows={true}
      >
        {RequestId}
        {Type}
        {LastDeploy}
        {Actions}
      </UITable>
    );
  }

  const groupDropdown = groups.map((group, index) => (<MenuItem key={index} onClick={() => setCurrentGroup(group)}>{group}</MenuItem>));

  return (
    <Row>
      <Col md={12} className="table-staged">
        <div className="page-header">
          <h2><ButtonGroup>
            <DropdownButton id="groups-dropdown" title={ currentGroup }>{ groupDropdown }</DropdownButton>
          </ButtonGroup> requests</h2>
        </div>
        {requestsSection}
      </Col>
    </Row>
  );
};

MyGroupRequests.propTypes = {
  groupRequests: PropTypes.arrayOf(PropTypes.object).isRequired,
  groups: PropTypes.arrayOf(PropTypes.string).isRequired,
  currentGroup: PropTypes.string
};

const mapStateToProps = (state) => {
  return {
    groupRequests: RequestsSelectors.getUserGroupRequests(state),
    groups: Utils.maybe(state.api.user, ['data', 'user', 'groups']) || [],
    currentGroup: state.ui.dashboard.currentGroup
  };
};

const mapDispatchToProps = (dispatch) => {
  return {
    setCurrentGroup: (group) => dispatch(SetDashboardGroup(group))
  };
}

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(MyGroupRequests);
