import React, { PropTypes } from 'react';
import { connect } from 'react-redux';
import { withRouter, routerShape } from 'react-router';
import rootComponent from '../../rootComponent';

import { Row, Col, Nav, NavItem, Label } from 'react-bootstrap';
import RequestDetailPage from '../requestDetail/RequestDetailPage';
import MetadataButton from '../common/MetadataButton';
import { refresh } from '../../actions/ui/groupDetail';
import ActionDropdown from './ActionDropdown';

const GroupDetail = (props) => {
  const { group, location, params, requestsNotFound, router } = props;
  const showRequestId = params.requestId || _.first(group.requestIds);

  const handleRequestSelect = (eventKey) => {
    router.push(`/group/${group.id}/${eventKey}`);
  };

  const metadata = !_.isEmpty(group.metadata) && (
    <MetadataButton title={group.id} metadata={group.metadata}>View Metadata</MetadataButton>
  );

  const requestPages = {};
  group.requestIds.forEach((requestId, index) => {
    requestPages[requestId] = (
      <RequestDetailPage
        index={index}
        key={requestId}
        params={{requestId}}
        location={location}
        showBreadcrumbs={false}
      />
    );
  });

  return (
    <div className="tabbed-page">
      <Row className="clearfix">
        <Col className="tab-col" sm={4} md={2}>
          <h3>Request Group</h3>
          <Row className="detail-header">
            <Col xs={10}>
              <h4>{group.id}</h4>
            </Col>
            <Col xs={2}>
              <ActionDropdown group={group} metadata={metadata} />
            </Col>
          </Row>
          <Nav
            activeKey={showRequestId}
            bsStyle="pills"
            onSelect={handleRequestSelect}
            stacked={true}
          >
            {group.requestIds.map(requestId =>
              <NavItem
                className="request-group-navitem"
                eventKey={requestId}
                key={requestId}
              >
                {requestId} {requestsNotFound.includes(requestId) && <Label bsStyle="danger">Deleted</Label>}
              </NavItem>
            )}
          </Nav>
        </Col>
        <Col sm={8} md={10}>
          {requestPages[showRequestId]}
        </Col>
      </Row>
    </div>
  );
};

GroupDetail.propTypes = {
  group: PropTypes.object,
  location: PropTypes.object,
  params: PropTypes.shape({
    requestId: PropTypes.string,
  }),
  requestsNotFound: PropTypes.array,
  router: routerShape,
};

const mapStateToProps = (state, ownProps) => {
  const group = _.find(state.api.requestGroups.data, (filterGroup) => filterGroup.id === ownProps.params.groupId);
  return ({
    notFound: !state.api.requestGroups.isFetching && !group,
    pathname: ownProps.location.pathname,
    group,
    requestsNotFound: Object.entries(state.api.request).filter((entry) => group.requestIds.includes(entry[0]) && entry[1].statusCode === 404).map((entry) => entry[0])
  });
};

export default withRouter(
  connect(mapStateToProps)(rootComponent(GroupDetail, refresh, false, false))
);
