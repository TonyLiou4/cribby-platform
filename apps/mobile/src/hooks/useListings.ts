import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  createListing,
  deleteListing,
  deleteListingImage,
  favoriteListing,
  getFavoriteStatus,
  getFavoriteListings,
  getListingById,
  getListingImages,
  getListings,
  getMyListings,
  unfavoriteListing,
  updateListing,
  updateListingStatus,
  uploadListingImage,
  reorderListingImages,
  type GetListingsParams,
} from '@/api/listings';
import type {
  CreateListingRequest,
  Listing,
  ManageableListingStatus,
  PagedResponse,
  PickedListingImage,
  UpdateListingRequest,
} from '@/types';

export function getInfiniteListingsQueryKey(params: Omit<GetListingsParams, 'page'> = {}) {
  return ['listings', 'infinite', params] as const;
}

function getNextListingsPage(lastPage: PagedResponse<Listing>) {
  if (lastPage.last) {
    return undefined;
  }

  const currentPage = lastPage.page ?? lastPage.number ?? 0;
  const nextPage = currentPage + 1;

  return nextPage < lastPage.totalPages ? nextPage : undefined;
}

export function useListings(params: GetListingsParams = {}) {
  return useQuery({
    queryKey: ['listings', params],
    queryFn: () => getListings(params),
  });
}

export function useInfiniteListings(params: Omit<GetListingsParams, 'page'> = {}) {
  return useInfiniteQuery({
    queryKey: getInfiniteListingsQueryKey(params),
    initialPageParam: 0,
    queryFn: ({ pageParam }) => getListings({ ...params, page: pageParam }),
    getNextPageParam: getNextListingsPage,
  });
}

export function useFavorites() {
  return useQuery({
    queryKey: ['favorites'],
    queryFn: getFavoriteListings,
  });
}

export function useCreateListing() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: CreateListingRequest) => createListing(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['listings'] });
    },
  });
}

export function useMyListings() {
  return useQuery({
    queryKey: ['my-listings'],
    queryFn: getMyListings,
  });
}

export function useUpdateListing(id: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: UpdateListingRequest) => updateListing(id, payload),
    onSuccess: (listing) => {
      queryClient.setQueryData(['listing', id], listing);
      void queryClient.invalidateQueries({ queryKey: ['my-listings'] });
      void queryClient.invalidateQueries({ queryKey: ['listings'] });
    },
  });
}

export function useUpdateListingStatus() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, status }: { id: number; status: ManageableListingStatus }) =>
      updateListingStatus(id, status),
    onSuccess: (listing) => {
      queryClient.setQueryData(['listing', listing.id], listing);
      void queryClient.invalidateQueries({ queryKey: ['my-listings'] });
      void queryClient.invalidateQueries({ queryKey: ['listings'] });
    },
  });
}

export function useDeleteListing() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: deleteListing,
    onSuccess: (_data, id) => {
      void queryClient.removeQueries({ queryKey: ['listing', id] });
      void queryClient.invalidateQueries({ queryKey: ['my-listings'] });
      void queryClient.invalidateQueries({ queryKey: ['listings'] });
    },
  });
}

export function useListingDetails(id?: number) {
  return useQuery({
    enabled: Boolean(id),
    queryKey: ['listing', id],
    queryFn: () => getListingById(id as number),
  });
}

export function useListingImages(listingId?: number) {
  return useQuery({
    enabled: Boolean(listingId),
    queryKey: ['listing-images', listingId],
    queryFn: () => getListingImages(listingId as number),
  });
}

export function useUploadListingImage(listingId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      image,
      onUploadProgress,
    }: {
      image: PickedListingImage;
      onUploadProgress?: (progress: number) => void;
    }) => uploadListingImage(listingId, { image, onUploadProgress }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['listing-images', listingId] });
      void queryClient.invalidateQueries({ queryKey: ['listing', listingId] });
      void queryClient.invalidateQueries({ queryKey: ['listings'] });
      void queryClient.invalidateQueries({ queryKey: ['my-listings'] });
    },
  });
}

export function useUploadListingImages() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      listingId,
      images,
      onUploadProgress,
    }: {
      listingId: number;
      images: PickedListingImage[];
      onUploadProgress?: (progress: number) => void;
    }) => {
      for (let index = 0; index < images.length; index += 1) {
        const image = images[index];
        await uploadListingImage(listingId, {
          image,
          onUploadProgress: (progress) => {
            onUploadProgress?.((index + progress) / images.length);
          },
        });
      }
    },
    onSuccess: (_data, variables) => {
      void queryClient.invalidateQueries({ queryKey: ['listing-images', variables.listingId] });
      void queryClient.invalidateQueries({ queryKey: ['listing', variables.listingId] });
      void queryClient.invalidateQueries({ queryKey: ['listings'] });
      void queryClient.invalidateQueries({ queryKey: ['my-listings'] });
    },
  });
}

export function useDeleteListingImage(listingId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (imageId: number) => deleteListingImage(listingId, imageId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['listing-images', listingId] });
      void queryClient.invalidateQueries({ queryKey: ['listing', listingId] });
      void queryClient.invalidateQueries({ queryKey: ['listings'] });
      void queryClient.invalidateQueries({ queryKey: ['my-listings'] });
    },
  });
}

export function useReorderListingImages(listingId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (imageIds: number[]) => reorderListingImages(listingId, imageIds),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['listing-images', listingId] });
      void queryClient.invalidateQueries({ queryKey: ['listing', listingId] });
    },
  });
}

export function useFavoriteStatus(listingId?: number) {
  return useQuery({
    enabled: Boolean(listingId),
    queryKey: ['favorite-status', listingId],
    queryFn: () => getFavoriteStatus(listingId as number),
  });
}

export function useToggleFavorite(listingId: number) {
  const queryClient = useQueryClient();
  const queryKey = ['favorite-status', listingId];

  return useMutation({
    mutationFn: async (isFavorite: boolean) => {
      if (isFavorite) {
        await unfavoriteListing(listingId);
        return false;
      }

      await favoriteListing(listingId);
      return true;
    },
    onMutate: async (isFavorite) => {
      await queryClient.cancelQueries({ queryKey });

      const previousStatus = queryClient.getQueryData<boolean>(queryKey);
      queryClient.setQueryData(queryKey, !isFavorite);

      return { previousStatus };
    },
    onError: (_error, _isFavorite, context) => {
      queryClient.setQueryData(queryKey, context?.previousStatus);
    },
    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey });
      void queryClient.invalidateQueries({ queryKey: ['favorites'] });
    },
  });
}

export function useRemoveFavorite() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: unfavoriteListing,
    onMutate: async (listingId) => {
      await queryClient.cancelQueries({ queryKey: ['favorites'] });

      const previousFavorites = queryClient.getQueryData<Awaited<ReturnType<typeof getFavoriteListings>>>([
        'favorites',
      ]);

      queryClient.setQueryData(
        ['favorites'],
        previousFavorites?.filter((listing) => listing.listingId !== listingId) ?? [],
      );
      queryClient.setQueryData(['favorite-status', listingId], false);

      return { previousFavorites };
    },
    onError: (_error, _listingId, context) => {
      queryClient.setQueryData(['favorites'], context?.previousFavorites);
    },
    onSettled: (_data, _error, listingId) => {
      void queryClient.invalidateQueries({ queryKey: ['favorites'] });
      void queryClient.invalidateQueries({ queryKey: ['favorite-status', listingId] });
    },
  });
}
