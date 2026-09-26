export type ListingStatus =
  | 'AVAILABLE'
  | 'PENDING'
  | 'OCCUPIED'
  | 'DRAFT'
  | 'ACTIVE'
  | 'PAUSED'
  | 'ARCHIVED';
export type RoomType = 'PRIVATE_ROOM' | 'SHARED_ROOM' | 'STUDIO' | 'OTHER';
export type GenderPreference = 'MALE' | 'FEMALE' | 'ANY';

export interface Listing {
  id: number;
  title: string;
  description: string;
  monthlyRent: number;
  city: string;
  neighborhood?: string | null;
  state: string;
  universityName?: string | null;
  roomType?: RoomType | null;
  genderPreference?: GenderPreference | null;
  availableFrom?: string | null;
  utilitiesIncluded?: boolean | null;
  exactAddress?: string | null;
  imageUrl?: string | null;
  imageUrls?: string[];
  status: ListingStatus;
  ownerId: number;
  createdAt: string;
  updatedAt: string;
}

export interface SavedListingResponse {
  favoriteId: number;
  listingId: number;
  ownerId: number;
  title: string;
  city: string;
  neighborhood?: string | null;
  monthlyRent: number;
  status: ListingStatus;
  roomType?: RoomType | null;
  genderPreference?: GenderPreference | null;
  availableFrom?: string | null;
  utilitiesIncluded?: boolean | null;
  universityName?: string | null;
  imageUrl?: string | null;
  imageUrls?: string[];
}

export type SavedListing = Listing & {
  favoriteId: number;
  listingId: number;
};

export interface ListingImage {
  id?: number;
  url: string;
  altText?: string | null;
  displayOrder?: number | null;
}

export type PickedListingImage = {
  uri: string;
  fileName: string;
  mimeType: string;
  fileSize?: number;
};

export interface CreateListingRequest {
  title: string;
  description: string;
  city: string;
  neighborhood: string;
  universityName?: string | null;
  monthlyRent: number;
  utilitiesIncluded: boolean;
  availableFrom: string;
  roomType: RoomType;
  genderPreference: GenderPreference;
  exactAddress?: string | null;
}

export type UpdateListingRequest = CreateListingRequest;
export type ManageableListingStatus = 'AVAILABLE' | 'PENDING' | 'OCCUPIED';
