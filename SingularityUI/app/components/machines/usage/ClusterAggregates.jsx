import React, { PropTypes } from 'react';
import { Link } from 'react-router';
import Utils from '../../../utils';
import Breakdown from '../../common/Breakdown';
import { HUNDREDTHS_PLACE } from '../Constants';
import Loader from "../../common/Loader";
import LabeledColumn from "./LabeledColumn";
import Aggregate from './Aggregate';

const SlaveAggregates = ({utilization, totalRequests}) => {
  return (
    <div>
      <h3>CPU</h3>
      <div className="row">
        <div className="col-md-2">
          <h4>Requests</h4>
          {utilization.numRequestsWithOverUtilizedCpu !== undefined ?
            <Breakdown
              total={totalRequests}
              data={[
                {
                  attribute: 'overCpu',
                  count: utilization.numRequestsWithOverUtilizedCpu,
                  type: 'danger',
                  label: 'Over-utilized',
                  link: '/requests/all/overUtilizedCpu/all/',
                  percent: (utilization.numRequestsWithOverUtilizedCpu / totalRequests) * 100
                },
                {
                  attribute: 'normal',
                  count: totalRequests - utilization.numRequestsWithUnderUtilizedCpu - utilization.numRequestsWithOverUtilizedCpu,
                  type: 'success',
                  label: 'Normal',
                  percent: ((totalRequests - utilization.numRequestsWithUnderUtilizedCpu - utilization.numRequestsWithOverUtilizedCpu) / totalRequests) * 100
                },
                {
                  attribute: 'underCpu',
                  count: utilization.numRequestsWithUnderUtilizedCpu,
                  type: 'warning',
                  label: 'Under-utilized',
                  link: '/requests/all/underUtilizedCpu/all/',
                  percent: (utilization.numRequestsWithUnderUtilizedCpu / totalRequests) * 100
                }
              ]}
            /> : <Loader fixed={false} />}
        </div>

        <LabeledColumn width={10}>
          <div className="row">
            <Aggregate width={3} value={Utils.roundTo(utilization.totalOverUtilizedCpu, HUNDREDTHS_PLACE)} label="Total Over-utilized CPUs" className="text-danger" />
            <Aggregate width={3} value={Utils.roundTo(utilization.avgOverUtilizedCpu, HUNDREDTHS_PLACE)} label="Avg Over-utilized CPUs" className="text-danger" />
            <Aggregate width={3} value={Utils.roundTo(utilization.minOverUtilizedCpu, HUNDREDTHS_PLACE)} label="Min Over-utilized CPUs" className="text-danger" />
            <Aggregate width={3} value={Utils.roundTo(utilization.maxOverUtilizedCpu, HUNDREDTHS_PLACE)} label="Max Over-utilized CPUs" className="text-danger" link={utilization.maxOverUtilizedCpuRequestId && `/request/${utilization.maxOverUtilizedCpuRequestId}`} />
          </div>
        </LabeledColumn>

        <LabeledColumn width={10}>
          <div className="row">
            <Aggregate width={3} value={Utils.roundTo(utilization.totalUnderUtilizedCpu, HUNDREDTHS_PLACE)} label="Total Under-utilized CPUs" className="text-warning" />
            <Aggregate width={3} value={Utils.roundTo(utilization.avgUnderUtilizedCpu, HUNDREDTHS_PLACE)} label="Avg Under-utilized CPUs" className="text-warning" />
            <Aggregate width={3} value={Utils.roundTo(utilization.minUnderUtilizedCpu, HUNDREDTHS_PLACE)} label="Min Under-utilized CPUs" className="text-warning" />
            <Aggregate width={3} value={Utils.roundTo(utilization.maxUnderUtilizedCpu, HUNDREDTHS_PLACE)} label="Max Under-utilized CPUs" className="text-warning" link={utilization.maxUnderUtilizedCpuRequestId && `/request/${utilization.maxUnderUtilizedCpuRequestId}`} />
          </div>
        </LabeledColumn>
      </div>

      <h3>Memory</h3>
      <div className="row">
        <div className="col-md-2">
          <h4>Requests</h4>
          {utilization.numRequestsWithUnderUtilizedMemBytes !== undefined ?
            <Breakdown
              total={totalRequests}
              data={[
                {
                  attribute: 'normal',
                  count: totalRequests - utilization.numRequestsWithUnderUtilizedMemBytes,
                  type: 'success',
                  label: 'Normal',
                  percent: ((totalRequests - utilization.numRequestsWithUnderUtilizedMemBytes) / totalRequests) * 100
                },
                {
                  attribute: 'underMem',
                  count: utilization.numRequestsWithUnderUtilizedMemBytes,
                  type: 'warning',
                  label: 'Under-utilized',
                  link: '/requests/all/underUtilizedMem/all/',
                  percent: (utilization.numRequestsWithUnderUtilizedMemBytes / totalRequests) * 100
                }
              ]}
            /> : <Loader fixed={false} />}
        </div>

        <LabeledColumn width={10}>
          <div className="row">
            <Aggregate width={3} value={Utils.humanizeFileSize(utilization.totalUnderUtilizedMemBytes)} label="Total Under-utilized Memory" className="text-warning" />
            <Aggregate width={3} value={Utils.humanizeFileSize(utilization.avgUnderUtilizedMemBytes)} label="Avg Under-utilized Memory" className="text-warning" />
            <Aggregate width={3} value={Utils.humanizeFileSize(utilization.minUnderUtilizedMemBytes)} label="Min Under-utilized Memory" className="text-warning" />
            <Aggregate width={3} value={Utils.humanizeFileSize(utilization.maxUnderUtilizedMemBytes)} label="Max Under-utilized Memory" className="text-warning" link={utilization.maxUnderUtilizedMemBytesRequestId && `/request/${utilization.maxUnderUtilizedMemBytesRequestId}`} />
          </div>
        </LabeledColumn>
      </div>

      <h3>Disk</h3>
      <div className="row">
        <div className="col-md-2">
          <h4>Requests</h4>
          {utilization.numRequestsWithUnderUtilizedDiskBytes !== undefined ?
            <Breakdown
              total={totalRequests}
              data={[
                {
                  attribute: 'normal',
                  count: totalRequests - utilization.numRequestsWithUnderUtilizedDiskBytes,
                  type: 'success',
                  label: 'Normal',
                  percent: ((totalRequests - utilization.numRequestsWithUnderUtilizedDiskBytes) / totalRequests) * 100
                },
                {
                  attribute: 'underDisk',
                  count: utilization.numRequestsWithUnderUtilizedDiskBytes,
                  type: 'warning',
                  label: 'Under-utilized',
                  link: '/requests/underUtilizedDisk/all/',
                  percent: (utilization.numRequestsWithUnderUtilizedDiskBytes / totalRequests) * 100
                }
              ]}
            /> : <Loader fixed={false} />}
        </div>

        <LabeledColumn width={10}>
          <div className="row">
            <Aggregate width={3} value={Utils.humanizeFileSize(utilization.totalUnderUtilizedDiskBytes)} label="Total Under-utilized Disk" className="text-warning" />
            <Aggregate width={3} value={Utils.humanizeFileSize(utilization.avgUnderUtilizedDiskBytes)} label="Avg Under-utilized Disk" className="text-warning" />
            <Aggregate width={3} value={Utils.humanizeFileSize(utilization.minUnderUtilizedDiskBytes)} label="Min Under-utilized Disk" className="text-warning" />
            <Aggregate width={3} value={Utils.humanizeFileSize(utilization.maxUnderUtilizedDiskBytes)} label="Max Under-utilized Disk" className="text-warning" link={utilization.maxUnderUtilizedDiskBytesRequestId && `/request/${utilization.maxUnderUtilizedDiskBytesRequestId}`} />
          </div>
        </LabeledColumn>
      </div>
    </div>
  );
};

SlaveAggregates.propTypes = {
  utilization: PropTypes.object.isRequired,
  totalRequests: PropTypes.number.isRequired
};

export default SlaveAggregates;
