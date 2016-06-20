import buildApiAction from './base';

export const FetchAction = buildApiAction('FETCH_REQUEST', (requestId) => `/requests/request/${ requestId }`);
export const makeSaveAction = (requestBody) => {
    return buildApiAction(
        'SAVE_REQUEST',
        "/requests",
        {
            method: 'POST',
            body: JSON.stringify(requestBody),
            headers: {'Content-Type': 'application/json'}
        }
    );
}
