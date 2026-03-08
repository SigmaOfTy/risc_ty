import React, { useState, useEffect, useCallback, useRef } from "react";
import { Link, useLocation } from "react-router-dom";
import { FileText, Cpu, MemoryStick, Code } from "lucide-react";
import navigationConfig from "@assets/config/navigation.json";
import "@styles/layout/sidebar.css";

interface Section {
  title: string;
  path: string;
  icon?: string;
  description?: string;
}
interface NavigationConfig {
  userGuide: { title: string; path: string; sections: Section[] };
  devGuide: { title: string; path: string; sections: Section[] };
}

const BREAKPOINT_MOBILE = 768;
const SIDEBAR_DEFAULT_WIDTH = 280;
const SIDEBAR_MIN_WIDTH = 160;
const SIDEBAR_MAX_WIDTH = 480;
const SIDEBAR_COLLAPSE_THRESHOLD = 80;

function deriveIsMobile(width: number): boolean {
  return width <= BREAKPOINT_MOBILE;
}

const Sidebar: React.FC = () => {
  const location = useLocation();
  const config = navigationConfig as unknown as NavigationConfig;

  const initialWidth = typeof window !== "undefined" ? window.innerWidth : 1280;
  const [sidebarWidth, setSidebarWidth] = useState<number>(
    deriveIsMobile(initialWidth) ? 0 : SIDEBAR_DEFAULT_WIDTH,
  );
  const [isMobile, setIsMobile] = useState<boolean>(
    deriveIsMobile(initialWidth),
  );

  const isMobileRef = useRef<boolean>(deriveIsMobile(initialWidth));
  const isDragging = useRef<boolean>(false);
  const dragStartX = useRef<number>(0);
  const dragStartWidth = useRef<number>(0);

  useEffect(() => {
    const ro = new ResizeObserver((entries) => {
      const width = entries[0]?.contentRect.width ?? window.innerWidth;
      const mobile = deriveIsMobile(width);
      const wasMobile = isMobileRef.current;
      isMobileRef.current = mobile;
      setIsMobile(mobile);
      if (mobile) {
        setSidebarWidth(0);
      } else if (wasMobile && !mobile) {
        setSidebarWidth(SIDEBAR_DEFAULT_WIDTH);
      }
    });
    ro.observe(document.documentElement);
    return () => ro.disconnect();
  }, []);

  const handleDragStart = useCallback(
    (e: React.MouseEvent) => {
      e.preventDefault();
      isDragging.current = true;
      dragStartX.current = e.clientX;
      dragStartWidth.current = sidebarWidth;
      document.body.style.cursor = "ew-resize";
      document.body.style.userSelect = "none";
    },
    [sidebarWidth],
  );

  useEffect(() => {
    const onMouseMove = (e: MouseEvent) => {
      if (!isDragging.current) return;
      const delta = e.clientX - dragStartX.current;
      const newWidth = dragStartWidth.current + delta;

      if (newWidth < SIDEBAR_COLLAPSE_THRESHOLD) {
        setSidebarWidth(0);
      } else {
        setSidebarWidth(
          Math.min(Math.max(newWidth, SIDEBAR_MIN_WIDTH), SIDEBAR_MAX_WIDTH),
        );
      }
    };

    const onMouseUp = () => {
      if (!isDragging.current) return;
      isDragging.current = false;
      document.body.style.cursor = "";
      document.body.style.userSelect = "";
    };

    window.addEventListener("mousemove", onMouseMove);
    window.addEventListener("mouseup", onMouseUp);
    return () => {
      window.removeEventListener("mousemove", onMouseMove);
      window.removeEventListener("mouseup", onMouseUp);
    };
  }, []);

  const handleBackdropClick = useCallback(() => {
    setSidebarWidth(0);
  }, []);

  const handleLinkClick = useCallback(() => {
    if (isMobileRef.current) setSidebarWidth(0);
  }, []);

  const isExpanded = sidebarWidth > 0;

  const getIcon = (iconName?: string): React.ReactElement => {
    const icons: Record<string, React.ReactElement> = {
      cpu: <Cpu size={15} />,
      memory: <MemoryStick size={15} />,
      code: <Code size={15} />,
      file: <FileText size={15} />,
    };
    return icons[iconName ?? "file"] ?? <FileText size={15} />;
  };

  const renderSectionList = (sections: Section[]) => (
    <ul className="app-sidebar__section-list">
      {sections.map((section) => (
        <li key={section.path} className="app-sidebar__section-item">
          <Link
            to={section.path}
            className={`app-sidebar__section-link ${
              location.pathname === section.path ? "active" : ""
            }`}
            title={section.description ?? section.title}
            onClick={handleLinkClick}
          >
            <span className="app-sidebar__section-icon">
              {getIcon(section.icon)}
            </span>
            <span className="app-sidebar__section-text">{section.title}</span>
          </Link>
        </li>
      ))}
    </ul>
  );

  return (
    <>
      <div
        className={`app-sidebar-wrapper ${
          isExpanded
            ? "app-sidebar-wrapper--expanded"
            : "app-sidebar-wrapper--collapsed"
        }`}
        style={!isMobile ? { width: isExpanded ? sidebarWidth : 0 } : undefined}
      >
        <aside
          className="app-sidebar"
          style={!isMobile ? { width: sidebarWidth } : undefined}
        >
          <div className="app-sidebar__header">
            <h3 className="app-sidebar__title">Navigation</h3>
          </div>

          <div className="app-sidebar__section-group">
            <div className="app-sidebar__group">
              <div className="app-sidebar__group-divider" />
              <h4 className="app-sidebar__group-title">User Guide</h4>
              {renderSectionList(config.userGuide.sections)}
            </div>
            <div className="app-sidebar__group">
              <div className="app-sidebar__group-divider" />
              <h4 className="app-sidebar__group-title">Developer Guide</h4>
              {renderSectionList(config.devGuide.sections)}
            </div>
          </div>
        </aside>

        <div
          className={`app-sidebar__drag-handle ${
            isDragging ? "app-sidebar__drag-handle--dragging" : ""
          }`}
          onMouseDown={handleDragStart}
          aria-label="Resize sidebar"
          role="separator"
          aria-orientation="vertical"
        />
      </div>

      {isMobile && isExpanded && (
        <div
          className="app-sidebar__backdrop"
          onClick={handleBackdropClick}
          aria-hidden="true"
        />
      )}
    </>
  );
};

export default Sidebar;
