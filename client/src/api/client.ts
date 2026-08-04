import axios from 'axios';
import { getStoredUser } from '../utils/currentUser';

export const apiClient = axios.create({
    baseURL: '/api',
    headers: {
        'Content-Type': 'application/json',
    },
});

apiClient.interceptors.request.use((config) => {
    const user = getStoredUser();
    if (user) {
        config.headers.set('X-User-Id', String(user.id));
    }
    return config;
});
