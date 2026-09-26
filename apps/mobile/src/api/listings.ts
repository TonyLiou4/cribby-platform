import { apiClient } from './client';

import type {
  CreateListingRequest,
  GenderPreference,
  Listing,
  ListingImage,
  ManageableListingStatus,
  PagedResponse,
  PickedListingImage,
  RoomType,
  SavedListing,
  SavedListingResponse,
  UpdateListingRequest,
} from '@/types';

export type GetListingsParams = {
  page?: number;
  size?: number;
  city?: string;
  universityName?: string;
  minRent?: number;
  maxRent?: number;
  roomType?: RoomType;
  genderPreference?: GenderPreference;
  availableFrom?: string;
  keyword?: string;
  sort?: string;
};

export async function getListings(params: GetListingsParams = {}) {
  const response = await apiClient.get<PagedResponse<Listing>>('/api/listings', {
    params,
  });

  return response.data;
}

function normalizeSavedListing(favorite: SavedListingResponse): SavedListing {
  return {
    id: favorite.listingId,
    favoriteId: favorite.favoriteId,
    listingId: favorite.listingId,
    ownerId: favorite.ownerId,
    title: favorite.title,
    description: '',
    city: favorite.city,
    neighborhood: favorite.neighborhood ?? null,
    state: '',
    universityName: favorite.universityName ?? null,
    monthlyRent: favorite.monthlyRent,
    status: favorite.status,
    roomType: favorite.roomType ?? null,
    genderPreference: favorite.genderPreference ?? null,
    availableFrom: favorite.availableFrom ?? null,
    utilitiesIncluded: favorite.utilitiesIncluded ?? null,
    exactAddress: null,
    imageUrl: favorite.imageUrl ?? null,
    imageUrls: favorite.imageUrls ?? [],
    createdAt: '',
    updatedAt: '',
  };
}

function normalizeFavoriteListings(favorites: SavedListingResponse[]) {
  const seenListingIds = new Set<number>();

  return favorites.reduce<SavedListing[]>((listings, favorite) => {
    if (seenListingIds.has(favorite.listingId)) {
      return listings;
    }

    const listing = normalizeSavedListing(favorite);

    seenListingIds.add(favorite.listingId);
    listings.push(listing);
    return listings;
  }, []);
}

export async function getFavoriteListings() {
  const response = await apiClient.get<PagedResponse<SavedListingResponse> | SavedListingResponse[]>(
    '/api/users/me/favorites',
  );

  if (Array.isArray(response.data)) {
    return normalizeFavoriteListings(response.data);
  }

  return normalizeFavoriteListings(response.data.content);
}

export async function createListing(payload: CreateListingRequest) {
  const response = await apiClient.post<Listing>('/api/listings', payload);
  return response.data;
}

export async function getMyListings() {
  const response = await apiClient.get<PagedResponse<Listing> | Listing[]>('/api/listings/me');

  if (Array.isArray(response.data)) {
    return response.data;
  }

  return response.data.content;
}

export async function updateListing(id: number, payload: UpdateListingRequest) {
  const response = await apiClient.put<Listing>(`/api/listings/${id}`, payload);
  return response.data;
}

export async function getListingById(id: number) {
  const response = await apiClient.get<Listing>(`/api/listings/${id}`);
  return response.data;
}

export async function getListingImages(listingId: number) {
  const response = await apiClient.get<(ListingImage | string)[]>(`/api/listings/${listingId}/images`);

  return response.data.map((image, index) => {
    if (typeof image === 'string') {
      return { id: index, url: image };
    }

    return image;
  });
}

type UploadListingImageOptions = {
  image: PickedListingImage;
  onUploadProgress?: (progress: number) => void;
};

type FormDataFile = {
  uri: string;
  name: string;
  type: string;
};

export async function uploadListingImage(
  listingId: number,
  { image, onUploadProgress }: UploadListingImageOptions,
) {
  const formData = new FormData();
  const file: FormDataFile = {
    uri: image.uri,
    name: image.fileName,
    type: image.mimeType,
  };

  formData.append('file', file as unknown as Blob);

  const response = await apiClient.post<ListingImage>(
    `/api/listings/${listingId}/images`,
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      onUploadProgress: (progressEvent) => {
        if (!onUploadProgress || !progressEvent.total) {
          return;
        }

        onUploadProgress(progressEvent.loaded / progressEvent.total);
      },
    },
  );

  return response.data;
}

export async function deleteListingImage(listingId: number, imageId: number) {
  await apiClient.delete(`/api/listings/${listingId}/images/${imageId}`);
}

export async function reorderListingImages(listingId: number, imageIds: number[]) {
  const response = await apiClient.patch<ListingImage[]>(
    `/api/listings/${listingId}/images/order`,
    { imageIds },
  );
  return response.data;
}

export async function getFavoriteStatus(listingId: number) {
  const response = await apiClient.get<boolean | { favorite?: boolean; favorited?: boolean; isFavorite?: boolean }>(
    `/api/listings/${listingId}/favorite-status`,
  );

  if (typeof response.data === 'boolean') {
    return response.data;
  }

  return Boolean(response.data.favorite ?? response.data.favorited ?? response.data.isFavorite);
}

export async function favoriteListing(listingId: number) {
  await apiClient.post(`/api/listings/${listingId}/favorite`);
}

export async function unfavoriteListing(listingId: number) {
  await apiClient.delete(`/api/listings/${listingId}/favorite`);
}

export async function updateListingStatus(id: number, status: ManageableListingStatus) {
  const response = await apiClient.patch<Listing>(`/api/listings/${id}/status`, { status });
  return response.data;
}

export async function deleteListing(id: number) {
  await apiClient.delete(`/api/listings/${id}`);
}
