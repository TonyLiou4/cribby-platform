import { create } from 'zustand';

type UiState = {
  isGlobalLoading: boolean;
  setGlobalLoading: (isGlobalLoading: boolean) => void;
};

export const useUiStore = create<UiState>((set) => ({
  isGlobalLoading: false,
  setGlobalLoading: (isGlobalLoading) => set({ isGlobalLoading }),
}));
