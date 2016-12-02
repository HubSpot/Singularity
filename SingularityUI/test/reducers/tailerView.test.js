import expect from 'expect';
import {
  SET_TAILER_GROUPS,
  ADD_TAILER_GROUP,
  REMOVE_TAILER_GROUP,
  PICK_TAILER_GROUP,
  buildTailerGroupInfo } from '../../app/actions/tailer';
import tailerViewReducer, { buildTailerId } from '../../app/reducers/tailerView';

const requestId1 = 'test-request-1';
const requestId2 = 'test-request-2';
const r1t1 = `${requestId1}-1`;
const r1t2 = `${requestId1}-2`;
const r2t1 = `${requestId2}-1`;
const logFilePath = 'stdout';
const logFilePathWithTaskIdVariable = '$TASK_ID/service.log';

describe('Reducers', () => {
  describe('tailerView', () => {
    it('should be properly initialized', () => {
      const initialState = tailerViewReducer(undefined, {});

      expect(initialState.tailerGroups).toEqual([]);
      expect(initialState.requestIds).toEqual([]);
      expect(initialState.taskIds).toEqual([]);
      expect(initialState.paths).toEqual([]);
    });

    it('should populate the tailerId field', () => {
      const initialState = tailerViewReducer(undefined, {});

      const action = {
        type: SET_TAILER_GROUPS,
        tailerGroups: [
          [
            buildTailerGroupInfo(r1t1, logFilePath),
            buildTailerGroupInfo(r1t2, logFilePath),
          ],
          [
            buildTailerGroupInfo(r2t1, logFilePathWithTaskIdVariable),
          ]
        ]
      };

      const newState = tailerViewReducer(initialState, action);

      expect(newState.tailerGroups[0][0].tailerId).toEqual(buildTailerId(0, r1t1, logFilePath));
      expect(newState.tailerGroups[0][1].tailerId).toEqual(buildTailerId(0, r1t2, logFilePath));
      expect(newState.tailerGroups[1][0].tailerId).toEqual(buildTailerId(1, r2t1, logFilePathWithTaskIdVariable));
      
    });

    it('should auto-populate the requestIds, taskIds, and paths fields', () => {
      const initialState = tailerViewReducer(undefined, {});

      const action = {
        type: SET_TAILER_GROUPS,
        tailerGroups: [
          [
            buildTailerGroupInfo(r1t1, logFilePath),
            buildTailerGroupInfo(r1t2, logFilePath),
          ],
          [
            buildTailerGroupInfo(r2t1, logFilePathWithTaskIdVariable),
          ]
        ]
      };

      const newState = tailerViewReducer(initialState, action);

      expect(newState.taskIds).toEqual([r1t1, r1t2, r2t1]);
      expect(newState.requestIds).toEqual([requestId1, requestId2]);
      expect(newState.paths).toEqual([logFilePathWithTaskIdVariable, logFilePath]);
    });

    it('should add tailer groups', () => {
      const initialState = tailerViewReducer(undefined, {});

      const action = {
        type: ADD_TAILER_GROUP,
        tailerGroup: [buildTailerGroupInfo(r1t1, logFilePath)]
      }

      const newState = tailerViewReducer(initialState, action);

      expect(newState.tailerGroups.length).toEqual(1);
      expect(newState.tailerGroups[0].length).toEqual(1);
      expect(newState.tailerGroups[0][0]).toEqual({...action.tailerGroup[0], tailerId: buildTailerId(0, r1t1, logFilePath)});
      expect(newState.requestIds).toEqual([requestId1]);
      expect(newState.taskIds).toEqual([r1t1]);
      expect(newState.paths).toEqual([logFilePath]);

      const action2 = {
        type: ADD_TAILER_GROUP,
        tailerGroup: [buildTailerGroupInfo(r1t2, logFilePath)]
      }

      const newState2 = tailerViewReducer(newState, action2);

      expect(newState2.tailerGroups.length).toEqual(2);
      expect(newState2.tailerGroups[1].length).toEqual(1);
      expect(newState2.tailerGroups[1][0]).toEqual({...action2.tailerGroup[0], tailerId: buildTailerId(1, r1t2, logFilePath)});
      expect(newState2.requestIds).toEqual([requestId1]);
      expect(newState2.taskIds).toEqual([r1t1, r1t2]);
      expect(newState2.paths).toEqual([logFilePath]);
    });

    it('should remove tailer groups', () => {
      const initialState = tailerViewReducer(undefined, {});

      const action = {
        type: SET_TAILER_GROUPS,
        tailerGroups: [
          [
            buildTailerGroupInfo(r1t1, logFilePath),
          ],
          [
            buildTailerGroupInfo(r1t2, logFilePath),
          ],
          [
            buildTailerGroupInfo(r2t1, logFilePathWithTaskIdVariable),
          ]
        ]
      };

      const newState = tailerViewReducer(initialState, action);

      expect(newState.tailerGroups.length).toEqual(3);

      const action2 = {
        type: REMOVE_TAILER_GROUP,
        tailerGroupIndex: 1
      };

      const newState2 = tailerViewReducer(newState, action2);

      expect(newState2.tailerGroups.length).toEqual(2);
      expect(newState2.tailerGroups.map((tg) => tg[0].taskId)).toEqual([r1t1, r2t1]);
      expect(newState2.requestIds).toEqual([requestId1, requestId2]);
      expect(newState2.taskIds).toEqual([r1t1, r2t1]);
      expect(newState2.paths).toEqual([logFilePathWithTaskIdVariable, logFilePath]);
    });

    it('should pick an individual tailer group', () => {
      const initialState = tailerViewReducer(undefined, {});

      const action = {
        type: SET_TAILER_GROUPS,
        tailerGroups: [
          [
            buildTailerGroupInfo(r1t1, logFilePath),
          ],
          [
            buildTailerGroupInfo(r1t2, logFilePath),
          ],
          [
            buildTailerGroupInfo(r2t1, logFilePathWithTaskIdVariable),
          ]
        ]
      };

      const newState = tailerViewReducer(initialState, action);

      expect(newState.tailerGroups.length).toEqual(3);

      const action2 = {
        type: PICK_TAILER_GROUP,
        tailerGroupIndex: 1
      };

      const newState2 = tailerViewReducer(newState, action2);

      expect(newState2.tailerGroups.length).toEqual(1);
      expect(newState2.tailerGroups[0][0].taskId).toEqual(r1t2);
      expect(newState2.requestIds).toEqual([requestId1]);
      expect(newState2.taskIds).toEqual([r1t2]);
      expect(newState2.paths).toEqual([logFilePath]);
    })
  });
});