import React, {PropTypes} from 'react';
import { connect } from 'react-redux';
import rootComponent from '../../rootComponent';

import { Row, Col, Tab, Nav, NavItem } from 'react-bootstrap';
import RequestDetailPage from '../requestDetail/RequestDetailPage';
import MetadataButton from '../common/MetadataButton';
import { refresh } from '../../actions/ui/groupDetail';
import ActionDropdown from './ActionDropdown';

const GroupDetail = ({group, location}) => {

  const metadata = !_.isEmpty(group.metadata) && (
    <MetadataButton title={group.id} metadata={group.metadata}>View Metadata</MetadataButton>
  );

  return (
    <div className="tabbed-page">
      <Tab.Container id="groupRequests" defaultActiveKey={0}>
        <Row className="clearfix">
          <Col className="tab-col" sm={2}>
            <h3>Request Group</h3>
            <Row className="detail-header">
              <Col xs={10}>
                <h4>{group.id}</h4>
              </Col>
              <Col xs={2}>
                  <ActionDropdown group={group} metadata={metadata} />
              </Col>
            </Row>

            <Nav bsStyle="pills" stacked={true}>
              {group.requestIds.map((requestId, index) => <NavItem key={index} eventKey={index}>{requestId}</NavItem>)}
            </Nav>
          </Col>
          <Col sm={10}>
            <Tab.Content animation={true}>
              {group.requestIds.map((requestId, index) => <Tab.Pane key={index} eventKey={index}><RequestDetailPage index={index} params={{requestId}} location={location} showBreadcrumbs={false} /></Tab.Pane>)}
            </Tab.Content>
          </Col>
        </Row>
      </Tab.Container>
    </div>
  );
};

GroupDetail.propTypes = {
  group: PropTypes.object,
  location: PropTypes.object,
  requests: PropTypes.object
};

const mapStateToProps = (state, ownProps) => {
  const group = _.find(state.api.requestGroups.data, (filterGroup) => filterGroup.id === ownProps.params.groupId);
  return ({
    notFound: !state.api.requestGroups.isFetching && !group,
    pathname: ownProps.location.pathname,
    group
  });
};

export default connect(mapStateToProps)(rootComponent(GroupDetail, refresh, false, false));
