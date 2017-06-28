import expect from 'expect';

import Utils from '../app/utils';

describe('Utils', () => {
  describe('getTaskDataFromTaskId()', () => {
    it('should grab all fields from a valid task id', () => {
      expect(Utils.getTaskDataFromTaskId('InternalEmailCronJobs-fsgj-92_13_3-1479418265674-1-hostname.example.com-us_east_1e'))
        .toEqual({
          id: 'InternalEmailCronJobs-fsgj-92_13_3-1479418265674-1-hostname.example.com-us_east_1e',
          rackId: 'us_east_1e',
          host: 'hostname.example.com',
          instanceNo: '1',
          startedAt: '1479418265674',
          deployId: '92_13_3',
          requestId: 'InternalEmailCronJobs-fsgj'
        });
    });
  });
});